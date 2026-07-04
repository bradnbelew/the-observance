package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import com.observance.watcher.util.TextFit;

/**
 * SENSORY — a boss bar appears for ONE player: "the land's attention." Authored title + color; shows
 * for a bounded time then auto-removes (never sticks, never UI-clutters). Used as a proximity-pull
 * cue or an in-character status line — not a quest tracker.
 *
 * <p>Payload:
 * <pre>{@code { "title":"...", "color":"PURPLE", "style":"SOLID", "progress":0.5, "seconds":8 } }</pre>
 */
public final class BossBarBeat extends AbstractBeat {

    @Override public String name() { return "boss_bar"; }
    @Override public String description() { return "A timed boss bar appears for one player — the land's attention."; }
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
        String title = clamp(p.string("title", ""));
        BarColor color = barColor(p.string("color", "PURPLE"));
        BarStyle style = barStyle(p.string("style", "SOLID"));
        double progress = Math.max(0.0, Math.min(1.0, p.number("progress", 1.0)));
        int seconds = Math.max(1, Math.min(60, p.integer("seconds", 8)));

        final BossBar bar;
        try {
            bar = org.bukkit.Bukkit.createBossBar(title, color, style);
        } catch (Throwable t) {
            return BeatResult.failed("create-bar");
        }
        bar.setProgress(progress);
        bar.addPlayer(pl);
        bar.setVisible(true);

        // Auto-remove so it can never linger.
        ctx.scheduler().runLaterSafe("beat.bossbar.remove", seconds * 20L, () -> {
            try {
                bar.removeAll();
                bar.setVisible(false);
            } catch (Throwable ignored) { }
        });
        return BeatResult.fired("bossbar=" + seconds + "s");
    }

    private static BarColor barColor(String name) {
        try { return BarColor.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (Throwable t) { return BarColor.PURPLE; }
    }

    private static BarStyle barStyle(String name) {
        try { return BarStyle.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (Throwable t) { return BarStyle.SOLID; }
    }

    // A boss bar is ONE non-wrapping line across the top of the screen — 256 characters would run
    // well past a typical client's screen width and just be cut off by the client, not the server.
    private static String clamp(String s) {
        return TextFit.clampLine(s, TextFit.HUD_LINE_CHARS);
    }
}
