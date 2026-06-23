package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;

/**
 * SENSORY — a per-player time or weather shift (client-only override; the world is untouched). For a
 * few seconds it is night, or raining, for ONE player. Deniable + uncanny. Auto-resets to the real
 * server time/weather so they snap back ("did the sky just...?").
 *
 * <p>Payload:
 * <pre>{@code { "mode":"time", "time":18000, "weather":"DOWNFALL", "seconds":8 } }</pre>
 * mode: "time" | "weather" | "both".
 */
public final class PrivateTimeShiftBeat extends AbstractBeat {

    @Override public String name() { return "private_time_shift"; }
    @Override public String description() { return "A brief per-player time/weather override, then it resets."; }
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
        String mode = p.string("mode", "time").trim().toLowerCase(java.util.Locale.ROOT);
        int seconds = Math.max(1, Math.min(30, p.integer("seconds", 8)));
        final java.util.UUID uuid = pl.getUniqueId();

        boolean did = false;
        if (mode.equals("time") || mode.equals("both")) {
            long time = clampTime(p.longValue("time", 18000L)); // default deep night
            pl.setPlayerTime(time, false);  // absolute (relative=false)
            did = true;
        }
        if (mode.equals("weather") || mode.equals("both")) {
            WeatherType wt = weather(p.string("weather", "DOWNFALL"));
            if (wt != null) { pl.setPlayerWeather(wt); did = true; }
        }
        if (!did) return BeatResult.skipped("nothing-to-do");

        ctx.scheduler().runLaterSafe("beat.timeshift.reset", seconds * 20L, () -> {
            Player still = Bukkit.getPlayer(uuid);
            if (still != null && still.isOnline()) {
                still.resetPlayerTime();
                still.resetPlayerWeather();
            }
        });
        return BeatResult.fired("shift=" + mode + "/" + seconds + "s");
    }

    private static long clampTime(long t) {
        long v = t % 24000L;
        return v < 0 ? v + 24000L : v;
    }

    private static WeatherType weather(String name) {
        try { return WeatherType.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (Throwable t) { return WeatherType.DOWNFALL; }
    }
}
