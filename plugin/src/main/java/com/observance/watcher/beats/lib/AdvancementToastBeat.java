package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.PerPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

/**
 * ACK — a hidden custom advancement toast ("the record notes you"). Grants an authored advancement
 * (shipped as a datapack with a toast/popup) to ONE player so the corner toast fires diegetically.
 * The advancement key is authored in the payload; if the advancement isn't installed, falls back to
 * a quiet per-player title so the acknowledgement still lands (graceful degradation).
 *
 * <p>Idempotent: re-granting an already-complete advancement is a no-op. The grant cannot be abused
 * (it only awards criteria; no item/effect).
 *
 * <p>Payload:
 * <pre>{@code { "advancement":"observance:record_notes_you", "fallback_title":"⟡", "fallback_subtitle":"the record notes you" } }</pre>
 */
public final class AdvancementToastBeat extends AbstractBeat {

    @Override public String name() { return "advancement_toast"; }
    @Override public String description() { return "A hidden custom advancement toast fires for one player — 'the record notes you'."; }
    @Override public BeatCategory category() { return BeatCategory.AMBIENT; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        return req.hasTarget();
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Player pl = target(req);
        if (pl == null) return BeatResult.skipped("no-target");
        BeatPayload p = req.payload();
        String advKey = p.string("advancement", null);

        if (advKey != null && !advKey.isBlank()) {
            NamespacedKey nk = parseKey(advKey, ctx.namespace());
            if (nk != null) {
                Advancement adv = org.bukkit.Bukkit.getAdvancement(nk);
                if (adv != null) {
                    AdvancementProgress prog = pl.getAdvancementProgress(adv);
                    if (prog.isDone()) return BeatResult.skipped("already-granted");
                    boolean awardedAny = false;
                    for (String crit : prog.getRemainingCriteria()) {
                        if (prog.awardCriteria(crit)) awardedAny = true;
                    }
                    if (awardedAny) return BeatResult.fired("advancement-granted");
                    // couldn't award — fall through to title fallback
                }
            }
        }

        // Fallback: quiet per-player title acknowledgement.
        String ft = p.string("fallback_title", "");
        String fs = p.string("fallback_subtitle", "");
        if (ft.isBlank() && fs.isBlank()) return BeatResult.skipped("no-advancement-no-fallback");
        PerPlayer.title(pl, ft, fs, 10, 50, 20);
        return BeatResult.fired("advancement-fallback-title");
    }

    private static NamespacedKey parseKey(String s, String defNamespace) {
        try {
            if (s.contains(":")) {
                String[] parts = s.split(":", 2);
                return new NamespacedKey(parts[0].toLowerCase(java.util.Locale.ROOT),
                        parts[1].toLowerCase(java.util.Locale.ROOT));
            }
            return new NamespacedKey(defNamespace, s.toLowerCase(java.util.Locale.ROOT));
        } catch (Throwable t) {
            return null;
        }
    }
}
