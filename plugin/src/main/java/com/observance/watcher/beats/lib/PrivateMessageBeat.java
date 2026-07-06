package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.PerPlayer;
import com.observance.watcher.util.TextFit;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * SENSORY — a word lands only for ONE player: normally an action-bar line above the hotbar
 * (transient, deniable), with explicit title/subtitle reserved for deliberately-authored boundary breaks.
 * The shortest, most personal beat. All text is authored.
 *
 * <p>Payload:
 * <pre>{@code
 * { "mode":"actionbar", "text":"..." }
 * { "mode":"title", "boundary_break":true, "title":"...", "subtitle":"...", "fade_in":10, "stay":40, "fade_out":20 }
 * // mode: "actionbar" | "title"; omitted defaults to actionbar.
 * }</pre>
 */
public final class PrivateMessageBeat extends AbstractBeat {

    @Override public String name() { return "private_message"; }
    @Override public String description() { return "A private action-bar word appears for one player; explicit title mode is reserved for boundary breaks."; }
    @Override public BeatCategory category() { return BeatCategory.AMBIENT; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (!req.hasTarget()) return false;
        BeatPayload p = req.payload();
        return p.has("title") || p.has("subtitle") || p.has("actionbar") || p.has("text");
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Player pl = target(req);
        if (pl == null) return BeatResult.skipped("no-target");
        BeatPayload p = req.payload();
        String mode = p.string("mode", "actionbar").trim().toLowerCase(Locale.ROOT);

        if (mode.equals("actionbar") || !p.bool("boundary_break", false)) {
            String text = p.string("actionbar", p.string("text", p.string("title", p.string("subtitle", ""))));
            if (text.isBlank()) return BeatResult.skipped("empty");
            PerPlayer.actionBar(pl, clamp(text));
            return BeatResult.fired(mode.equals("actionbar") ? "actionbar" : "title-demoted-actionbar");
        }

        String title = p.string("title", p.string("text", ""));
        String subtitle = p.string("subtitle", "");
        if (title.isBlank() && subtitle.isBlank()) return BeatResult.skipped("empty");
        int fadeIn = clampTicks(p.integer("fade_in", 10));
        int stay = clampTicks(p.integer("stay", 40));
        int fadeOut = clampTicks(p.integer("fade_out", 20));
        PerPlayer.title(pl, clamp(title), clamp(subtitle), fadeIn, stay, fadeOut);
        return BeatResult.fired("title");
    }

    private static int clampTicks(int t) { return Math.max(0, Math.min(20 * 60, t)); }
    // Title/subtitle/action-bar are all ONE non-wrapping line — 256 characters would run well past
    // what a client actually shows, clipped silently on the client, not the server.
    private static String clamp(String s) {
        return TextFit.clampLine(s, TextFit.HUD_LINE_CHARS);
    }
}
