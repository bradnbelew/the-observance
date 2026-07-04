package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.PerPlayer;
import com.observance.watcher.util.TextFit;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * THE CLOSING — the finale death/kick beat (design/FINALE-THE-RELEASE.md). Enqueued ONCE by the
 * showrunner when the group performs the release act. The world visibly "dies" server-wide for a few
 * seconds (fog + darkness + the whisper swell, per player), then EVERY online player is KICKED with the
 * Seventh's composed sign-off ({@code kick_line}) as the vanilla disconnect-screen message — the game
 * reaching out of the fiction, signing off as a person the group just freed.
 *
 * <p><b>SIMULATED death, never real shutdown (Ethan's locked decision).</b> Paper keeps running; the
 * operator controls re-entry. The theater is ephemeral per-player sensory effects (no world griefing,
 * nothing saved over). <b>KICK, never ban.</b> Optionally (config {@code closing.whitelist-after}) the
 * whitelist is turned ON right after the kick so the group cannot rejoin until the operator re-opens it
 * ("nothing here to come back to") — still not a ban, fully reversible.
 *
 * <p><b>Once, safely.</b> {@link AbstractBeat} makes it idempotent on the beat id (the world ends exactly
 * once). It is hard-gated upstream: the showrunner only enqueues it after the release act
 * ({@code record_released}) + a decided fate, so it can never fire early. Config {@code closing.enabled}
 * false → the beat SKIPS the kick entirely (the operator stages it manually) while the farewell still
 * posts. Body in Safety; a player logging off mid-theater is a no-op; the kick is scheduled on MAIN.
 *
 * <p><b>Lore-agnostic.</b> The only text it shows is {@code kick_line} from the payload (composed by
 * voice.ts / finale.ts). It hardcodes no prose.
 */
public final class TheClosingBeat extends AbstractBeat {

    @Override public String name() { return "the_closing"; }
    @Override public String description() { return "The record closes: the world dies server-wide, then every player is kicked with the Seventh's sign-off."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    /** A safe fallback if the payload somehow carries no composed sign-off (should never happen). */
    private static final String FALLBACK_KICK = "the record is closed. thank you for coming down.";

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        // Server-wide; needs no target or site. Always enactable (the enabled gate is honored in doEnact so
        // that a disabled config still consumes the beat cleanly rather than leaving it pending forever).
        return true;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        BeatPayload p = req.payload();
        final String kickLine = TextFit.clampLine(
                firstNonBlank(p.string("kick_line", ""), FALLBACK_KICK), 256);

        // Operator kill-switch: post-farewell, but no auto-kick. The beat is still "fired" (consumed), so
        // it never re-runs; the operator stages the kick by hand if they want it.
        if (!ctx.config().closingEnabled()) {
            ctx.safety().info("closing", "the_closing fired but closing.enabled=false — farewell stands, no auto-kick.");
            return BeatResult.fired("closing-disabled-no-kick");
        }

        final int theaterSeconds = Math.max(0, ctx.config().closingTheaterSeconds());
        final boolean whitelistAfter = ctx.config().closingWhitelistAfter();

        // 1) The world dies — per-player sensory theater, server-wide. Fog + darkness + the whisper swell,
        //    for every online player at once. Ephemeral (fog/darkness auto-expire); nothing is griefed.
        ctx.safety().run("closing.theater", () -> {
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl == null || !pl.isOnline()) continue;
                final UUID id = pl.getUniqueId();
                ctx.safety().run("closing.theater.player", () -> {
                    Player pp = Bukkit.getPlayer(id);
                    if (pp == null || !pp.isOnline()) return;
                    // fog + darkness for the whole theater window (+1s tail so it doesn't lift before the kick).
                    PerPlayer.fog(pp, theaterSeconds + 1, 1);
                    try {
                        pp.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.DARKNESS,
                                (theaterSeconds + 1) * 20, 0, true, false, false));
                    } catch (Throwable ignored) { }
                    // the whisper swell (the pack sound; silent-safe if the pack isn't installed).
                    try { PerPlayer.namedSound(pp, "observance:whisper", 0.9f, 0.6f); } catch (Throwable ignored) { }
                    // the light draws back around them.
                    ctx.safety().run("closing.dim", () -> PerPlayer.dimLightAround(pp));
                });
            }
        });

        // 2) The kick — after the theater window, kick EVERY online player with the composed sign-off, on
        //    MAIN. A player who left during the theater is simply skipped.
        ctx.scheduler().runLaterSafe("closing.kick", theaterSeconds * 20L, () -> {
            final Component reason = Component.text(kickLine);
            int kicked = 0;
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl == null || !pl.isOnline()) continue;
                try { pl.kick(reason); kicked++; }
                catch (Throwable t) {
                    // Older API fallback: the deprecated string kick. Never let one client fault strand the rest.
                    try { pl.kickPlayer(kickLine); kicked++; } catch (Throwable ignored) { }
                }
            }
            if (whitelistAfter) {
                // Turn the whitelist ON so the group cannot rejoin until the operator re-opens it. NOT a ban.
                ctx.safety().run("closing.whitelist", () -> Bukkit.setWhitelist(true));
            }
            ctx.safety().info("closing", "the record is closed — kicked " + kicked + " player(s)"
                    + (whitelistAfter ? " + whitelist on (re-open to allow rejoin)" : ""));
        });

        return BeatResult.fired("closing-armed:" + theaterSeconds + "s");
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}
