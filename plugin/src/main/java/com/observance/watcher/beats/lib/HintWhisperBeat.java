package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.PerPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Locale;

/**
 * IN-WORLD HINT DELIVERY (findability). A hint currently only reaches Discord; this beat lands it IN
 * THE WORLD, privately, for one stuck player — a Watcher-register whisper. It carries the hint BODY
 * text from the payload primarily as a short-lived per-player {@link TextDisplay} floating in front of
 * them, revealed to that one client (via
 * {@link PerPlayer#showEntityTo}). No one standing next to them sees or hears it — the help arrives as
 * a private nudge, in the watcher's own register, not a server announcement.
 *
 * <p><b>DIRECTED</b>: the showrunner / findability logic enqueues this against a specific
 * {@code mc_uuid} when a player is judged stuck. It bypasses ambient spacing (the help is intentional)
 * but still counts toward the drama window.
 *
 * <p><b>Reveal-safe + behaviour-safe</b> like {@link NameOnWallBeat}: the optional display is spawned
 * invisible-to-everyone then revealed to the one target, non-persistent (no orphans), PDC-tagged, and
 * every follow-up re-resolves the player by UUID and validity-checks the display. A logout mid-whisper
 * simply removes it. The optional text half (action-bar/title) is inherently transient and per-player.
 *
 * <p>Payload:
 * <pre>{@code
 * {
 *   "body": "Try the lectern beneath the stair.",  // REQUIRED — the hint text (from findability/showrunner)
 *   "tier": 2,                                       // optional hint strength 1..3 (colours + framing; audit)
 *   "mode": "display",      // "display" | "actionbar" | "title" | "both" (title+display). Default "display".
 *   "prefix": "…a whisper…",// optional subtitle/lead line under a title (default a soft ellipsis)
 *   "seconds": 6,           // display auto-despawn + title stay window (clamped 2..15)
 *   "distance": 3,          // blocks in front for the display (clamped 2..8)
 *   "rune_font": false,     // carve the display in observance:runes (default false — a hint should READ)
 *   "color": "#c9b458",     // hex/named colour override (default derived from tier)
 *   "look_away_despawn": false, // erase the display the instant they look away (default false — a hint
 *                               //   should be readable; the hard timer ends it)
 *   "billboard": true       // display faces the player (default true)
 * }
 * }</pre>
 */
public final class HintWhisperBeat extends AbstractBeat {

    private static final String RUNE_FONT = "observance:runes";
    private static final int WATCH_PERIOD_TICKS = 4;
    private static final int MAX_BODY = 180;

    /** Tier → glyph colour (warmer/brighter as the hint gets more explicit). */
    private static TextColor tierColor(int tier) {
        return switch (tier) {
            case 1 -> TextColor.color(0x8a, 0x8f, 0x99);   // faint grey — barely a nudge
            case 3 -> TextColor.color(0xe0, 0xc0, 0x50);   // clear gold — an outright pointer
            default -> TextColor.color(0xc9, 0xb4, 0x58);  // muted amber — the standard whisper (tier 2)
        };
    }

    @Override public String name() { return "hint_whisper"; }
    @Override public String description() { return "Delivers a private in-world hint as a floating note by default; explicit title/action-bar modes remain available."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        // Needs an online target and a non-blank hint body. The display half additionally needs a
        // placement, but canEnact stays permissive: explicit text modes can still deliver the hint.
        if (!req.hasTarget()) return false;
        String body = req.payload().string("body", "");
        return body != null && !body.isBlank();
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Player pl = target(req);
        if (pl == null) return BeatResult.skipped("no-target");

        BeatPayload p = req.payload();
        String rawBody = p.string("body", "");
        if (rawBody.isBlank()) return BeatResult.skipped("empty");

        final int tier = Math.max(1, Math.min(3, p.integer("tier", 2)));
        final String mode = p.string("mode", "display").trim().toLowerCase(Locale.ROOT);
        final int seconds = Math.max(2, Math.min(15, p.integer("seconds", 6)));
        final TextColor color = p.has("color")
                ? NameOnWallBeat.colorOf(p.string("color", null))
                : tierColor(tier);
        final String body = clamp(rawBody.replace("%name%", pl.getName()));

        boolean boundaryBreak = p.bool("boundary_break", false);
        boolean wantTitle = mode.equals("actionbar") || ((mode.equals("title") || mode.equals("both")) && boundaryBreak);
        boolean wantDisplay = mode.equals("display") || mode.equals("both") || (mode.equals("title") && !boundaryBreak);

        boolean deliveredText = false;
        boolean deliveredDisplay = false;

        // --- Text half: the Watcher-register whisper (transient, per-player, deniable) ---
        if (wantTitle) {
            if (mode.equals("actionbar")) {
                PerPlayer.actionBar(pl, body);
                deliveredText = true;
            } else {
                // A soft lead line, then the hint body as the title. Keep the title readable → longer stay.
                String prefix = clamp(p.string("prefix", "…a whisper…"));
                int stay = seconds * 20;
                PerPlayer.title(pl, body, prefix, 10, stay, 20);
                deliveredText = true;
            }
        }

        // --- Display half: a short-lived floating rune/note in front of them ---
        if (wantDisplay) {
            deliveredDisplay = spawnDisplay(ctx, req, pl, body, color, tier, seconds, p);
        }

        if (!deliveredText && !deliveredDisplay) {
            return BeatResult.skipped("nothing-delivered");
        }

        // Audit tier out-of-band (the showrunner may want to see which strength landed).
        final int firedTier = tier;
        ctx.scheduler().runAsyncSafe("beat.hint_whisper.audit",
                () -> ctx.safety().info("beat.hint_whisper",
                        "target=" + pl.getName() + " tier=" + firedTier + " mode=" + mode));

        return BeatResult.fired("hint-tier" + tier);
    }

    /**
     * Spawn the per-player hint display in front of the player, reveal it to only them, and schedule its
     * despawn (+ optional look-away erase). Returns false if it could not be placed (the text half may
     * still have delivered). MAIN thread.
     */
    private boolean spawnDisplay(BeatContext ctx, BeatRequest req, Player pl, String body,
                                 TextColor color, int tier, int seconds, BeatPayload p) {
        Location spot = placement(pl, p);
        if (spot == null) return false;

        final boolean runeFont = p.bool("rune_font", false);
        final boolean billboard = p.bool("billboard", true);
        final boolean lookAway = p.bool("look_away_despawn", false);

        Component label = Component.text(body).color(color);
        if (runeFont) label = label.font(net.kyori.adventure.key.Key.key(RUNE_FONT));
        final Component finalLabel = label;

        TextDisplay display;
        try {
            display = spot.getWorld().spawn(spot, TextDisplay.class, td -> {
                td.setVisibleByDefault(false);            // invisible to everyone until revealed to the one
                td.setPersistent(false);                  // no orphans across restart/chunk-save
                td.text(finalLabel);
                td.setBillboard(billboard ? Display.Billboard.CENTER : Display.Billboard.FIXED);
                td.setSeeThrough(false);
                td.setShadowed(true);
                td.setDefaultBackground(false);
                try { td.setBackgroundColor(org.bukkit.Color.fromARGB(0x50, 0, 0, 0)); } catch (Throwable ignored) { }
                td.setBrightness(new Display.Brightness(15, 15));
                td.setViewRange(0.6f);
                td.setTransformation(new Transformation(
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f(0f, 0f, 0f, 1f),
                        new Vector3f(1.0f, 1.0f, 1.0f),
                        new AxisAngle4f(0f, 0f, 0f, 1f)));
            });
        } catch (Throwable t) {
            return false;
        }
        if (display == null || !display.isValid()) return false;

        try {
            display.getPersistentDataContainer().set(
                    key(ctx, "beat_entity"), PersistentDataType.STRING, req.beatId());
            display.getPersistentDataContainer().set(
                    key(ctx, "beat_owner"), PersistentDataType.STRING, req.targetUuid().toString());
        } catch (Throwable ignored) { }

        PerPlayer.showEntityTo(ctx.plugin(), pl, display);

        final java.util.UUID displayId = display.getUniqueId();
        final java.util.UUID targetId = req.targetUuid();

        ctx.scheduler().runLaterSafe("beat.hint_whisper.despawn", seconds * 20L,
                () -> removeDisplay(displayId));

        if (lookAway) {
            final Location watchAt = spot.clone();
            final org.bukkit.scheduler.BukkitTask[] holder = new org.bukkit.scheduler.BukkitTask[1];
            holder[0] = ctx.scheduler().runTimerSafe("beat.hint_whisper.watch",
                    WATCH_PERIOD_TICKS, WATCH_PERIOD_TICKS, () -> {
                        org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(displayId);
                        if (e == null || !e.isValid()) { cancel(holder[0]); return; }
                        Player watcher = org.bukkit.Bukkit.getPlayer(targetId);
                        if (watcher == null || !watcher.isOnline()
                                || watcher.getWorld() == null
                                || !watcher.getWorld().equals(watchAt.getWorld())) {
                            removeDisplay(displayId);
                            cancel(holder[0]);
                            return;
                        }
                        boolean looking = ctx.safety().call("beat.hint_whisper.los",
                                () -> ctx.reveal().hasLineOfSight(watcher, watchAt), Boolean.TRUE);
                        if (!Boolean.TRUE.equals(looking)) {
                            removeDisplay(displayId);
                            cancel(holder[0]);
                        }
                    });
        }
        return true;
    }

    /**
     * Where to float the hint: a point {@code distance} blocks along the player's gaze at eye level, in a
     * loaded chunk. Same approach as {@link NameOnWallBeat} — a self-lit billboard reads fine floating.
     * Returns null if unresolvable. MAIN thread.
     */
    private static Location placement(Player pl, BeatPayload p) {
        if (pl == null) return null;
        int distance = Math.max(2, Math.min(8, p.integer("distance", 3)));
        Location eye = pl.getEyeLocation();
        if (eye.getWorld() == null) return null;
        org.bukkit.util.Vector dir = eye.getDirection().clone();
        dir.setY(dir.getY() * 0.3);                  // flatten so the note sits near eye level
        if (dir.lengthSquared() < 1.0e-6) return null;
        dir.normalize();
        Location spot = eye.clone().add(dir.multiply(distance));
        if (!spot.getWorld().isChunkLoaded(spot.getBlockX() >> 4, spot.getBlockZ() >> 4)) return null;
        org.bukkit.util.Vector back = pl.getLocation().toVector().subtract(spot.toVector());
        if (back.lengthSquared() > 1.0e-6) spot.setDirection(back);
        return spot;
    }

    private static void removeDisplay(java.util.UUID id) {
        try {
            org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(id);
            if (e != null && e.isValid()) e.remove();
        } catch (Throwable ignored) { }
    }

    private static void cancel(org.bukkit.scheduler.BukkitTask task) {
        try { if (task != null) task.cancel(); } catch (Throwable ignored) { }
    }

    private static String clamp(String s) {
        if (s == null) return "";
        return s.length() > MAX_BODY ? s.substring(0, MAX_BODY) : s;
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (server-free; mirrors the repo idiom).  */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the pure helpers: tier→colour never returns null across the clamp range and the out-of-range
     * tier falls to the standard amber; body clamping bounds a malformed payload. A regression would NPE
     * the whisper's colour or let an unbounded hint onto the screen.
     */
    static boolean hintWhisperSelfTest() {
        if (tierColor(1) == null || tierColor(2) == null || tierColor(3) == null) return false;
        // out-of-range tiers fall to the tier-2 default (via the switch default).
        if (!tierColor(0).equals(tierColor(2))) return false;
        if (!tierColor(9).equals(tierColor(2))) return false;
        if (clamp(null).length() != 0) return false;
        if (clamp("x".repeat(500)).length() != MAX_BODY) return false;
        return clamp("short").equals("short");
    }
}
