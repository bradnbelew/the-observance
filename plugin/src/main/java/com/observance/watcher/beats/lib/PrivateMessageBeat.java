package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.PerPlayer;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * SENSORY — a word lands only for ONE player: a fading title/subtitle, or an action-bar line above
 * the hotbar (transient, deniable). The shortest, most personal beat. All text is authored.
 *
 * <p>Payload:
 * <pre>{@code
 * { "mode":"title", "title":"...", "subtitle":"...", "fade_in":10, "stay":40, "fade_out":20 }
 * // mode: "title" | "actionbar"
 * }</pre>
 */
public final class PrivateMessageBeat extends AbstractBeat {

    @Override public String name() { return "private_message"; }
    @Override public String description() { return "A title/subtitle or action-bar word appears for one player."; }
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
        String mode = p.string("mode", "title").trim().toLowerCase(Locale.ROOT);

        if (mode.equals("actionbar")) {
            String text = p.string("actionbar", p.string("text", ""));
            if (text.isBlank()) return BeatResult.skipped("empty");
            PerPlayer.actionBar(pl, clamp(text));
            return BeatResult.fired("actionbar");
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
    private static String clamp(String s) {
        if (s == null) return "";
        return s.length() > 256 ? s.substring(0, 256) : s;
    }
}
