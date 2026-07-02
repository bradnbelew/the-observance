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

/**
 * PERSONALIZED — the signature "it knows ME" scare (INTEGRATION §SIGNATURE #1). A {@link TextDisplay}
 * carrying the TARGET player's OWN name, carved in the keepers' rune font, materialises on a wall a few
 * blocks in front of them — and <b>only that one player can see it</b>. To everyone else the wall is
 * blank. It self-erases after a short timer, or the instant the player looks away (the name was never
 * really there).
 *
 * <p><b>Why this is the centerpiece.</b> A server-wide announcement is a game event; a message on the
 * wall that carries your name and that no one standing next to you can see is a private haunting. This
 * beat is the plugin's proof of the per-player illusion primitives — it is built entirely on
 * {@link PerPlayer#showEntityTo} (per-player entity visibility) so the display is a real server entity
 * that is revealed to exactly one client.
 *
 * <p><b>Reveal-safe by construction.</b> The display is spawned {@code setVisibleByDefault(false)} so it
 * is invisible to EVERYONE for the frame it exists before we reveal it to the target — no bystander ever
 * catches it popping in. The target "discovers" it: we place it where they're facing and let them turn
 * to read it.
 *
 * <p><b>Behavior-safe by construction.</b> The entity is tagged in PDC as a beat entity, is never
 * persistent (dies with a chunk unload / restart — no orphan can survive a crash), and every scheduled
 * follow-up re-resolves the player by UUID and null-checks the display's validity. A logout, death, or
 * world-change mid-scare simply removes the display and ends the beat — never an NPE, never a stranded
 * entity, never a name left hanging on a wall.
 *
 * <p>Payload (all optional):
 * <pre>{@code
 * {
 *   "distance": 3,            // blocks in front of the player to place the name (clamped 2..8)
 *   "seconds": 4,            // auto-despawn timer (clamped 1..12)
 *   "look_away_despawn": true,// erase the instant they stop looking at it (default true)
 *   "rune_font": true,        // carve in observance:runes (default true); false = plain text
 *   "text": "%name%",        // override; "%name%" is replaced with the target's own name
 *   "color": "#8a1c1c",       // hex or named colour for the glyphs (default dark blood)
 *   "billboard": true,        // face the player as they move (default true)
 *   "glow": false             // outline glow (default false — a rune should be barely-there)
 * }
 * }</pre>
 *
 * <p>This beat is {@link BeatCategory#PERSONALIZED}: it targets one player and is fired by the
 * showrunner / drama budget against a specific dossier, not synthesised ambiently for the room.
 */
public final class NameOnWallBeat extends AbstractBeat {

    /** The resource-pack font carrying the keepers' rune alphabet (shared with {@link KeeperNpcBeat}). */
    private static final String RUNE_FONT = "observance:runes";

    /** Poll cadence (ticks) for the look-away watchdog — fast enough to feel instant, cheap enough to run. */
    private static final int WATCH_PERIOD_TICKS = 4;

    /** Default blood-dark glyph colour when none is authored. */
    private static final TextColor DEFAULT_COLOR = TextColor.color(0x8a, 0x1c, 0x1c);

    @Override public String name() { return "name_on_wall"; }
    @Override public String description() { return "The player's own name appears in runes on a wall — only they can see it."; }
    @Override public BeatCategory category() { return BeatCategory.PERSONALIZED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        // Only plausible with an online target and a place to put the name in front of them.
        return req.hasTarget() && placement(req) != null;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Player pl = target(req);
        if (pl == null) return BeatResult.skipped("no-target");

        Location spot = placement(req);
        if (spot == null) return BeatResult.skipped("no-placement");

        BeatPayload p = req.payload();
        final boolean runeFont = p.bool("rune_font", true);
        final boolean billboard = p.bool("billboard", true);
        final boolean glow = p.bool("glow", false);
        final boolean lookAway = p.bool("look_away_despawn", true);
        final int seconds = Math.max(1, Math.min(12, p.integer("seconds", 4)));
        final TextColor color = colorOf(p.string("color", null));

        // The carved text: "%name%" (default) becomes the target's own display name — the whole point.
        String rawText = p.string("text", "%name%");
        final String shown = clamp(rawText.replace("%name%", pl.getName()));

        Component label = Component.text(shown).color(color);
        if (runeFont) {
            label = label.font(net.kyori.adventure.key.Key.key(RUNE_FONT));
        }
        final Component finalLabel = label;

        TextDisplay display;
        try {
            display = spot.getWorld().spawn(spot, TextDisplay.class, td -> {
                // Invisible to EVERYONE at spawn — we reveal it to exactly one client below, so no
                // bystander (and not even the target, for the pre-reveal frame) sees it pop in.
                td.setVisibleByDefault(false);
                td.setPersistent(false);                 // never survive a restart / chunk save → no orphans
                td.text(finalLabel);
                td.setBillboard(billboard ? Display.Billboard.CENTER : Display.Billboard.FIXED);
                td.setSeeThrough(false);
                td.setShadowed(true);
                td.setDefaultBackground(false);
                try { td.setBackgroundColor(org.bukkit.Color.fromARGB(0)); } catch (Throwable ignored) { }
                td.setBrightness(new Display.Brightness(15, 15));   // self-lit so the dark wall can't hide it
                td.setGlowing(glow);
                td.setViewRange(0.35f);                   // small — a private, close apparition
                // A gentle scale so the runes read at wall distance without dominating.
                td.setTransformation(new Transformation(
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f(0f, 0f, 0f, 1f),
                        new Vector3f(1.4f, 1.4f, 1.4f),
                        new AxisAngle4f(0f, 0f, 0f, 1f)));
            });
        } catch (Throwable t) {
            return BeatResult.failed("spawn-error");
        }
        if (display == null || !display.isValid()) {
            return BeatResult.failed("no-display");
        }

        // Tag as a beat entity so cleanup sweeps recognise + can remove it (anti-orphan / anti-grief).
        try {
            display.getPersistentDataContainer().set(
                    key(ctx, "beat_entity"), PersistentDataType.STRING, req.beatId());
            display.getPersistentDataContainer().set(
                    key(ctx, "beat_owner"), PersistentDataType.STRING, req.targetUuid().toString());
        } catch (Throwable ignored) { }

        // Reveal to the ONE target — the whole illusion. Everyone else keeps seeing a blank wall.
        PerPlayer.showEntityTo(ctx.plugin(), pl, display);

        final java.util.UUID displayId = display.getUniqueId();
        final java.util.UUID targetId = req.targetUuid();

        // Hard despawn timer — the name never lingers past the beat's window, even if the watchdog is off.
        ctx.scheduler().runLaterSafe("beat.name_on_wall.despawn", seconds * 20L,
                () -> removeDisplay(displayId));

        // Look-away watchdog: erase the instant they stop looking at it, so a turn-back finds a blank
        // wall ("was that ever there?"). Cancels itself once the display is gone. Opt-out via payload.
        if (lookAway) {
            final Location watchAt = spot.clone();
            final org.bukkit.scheduler.BukkitTask[] holder = new org.bukkit.scheduler.BukkitTask[1];
            holder[0] = ctx.scheduler().runTimerSafe("beat.name_on_wall.watch",
                    WATCH_PERIOD_TICKS, WATCH_PERIOD_TICKS, () -> {
                        org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(displayId);
                        if (e == null || !e.isValid()) {          // already despawned → stop watching
                            cancel(holder[0]);
                            return;
                        }
                        Player watcher = org.bukkit.Bukkit.getPlayer(targetId);
                        if (watcher == null || !watcher.isOnline()
                                || !watcher.getWorld().equals(watchAt.getWorld())) {
                            removeDisplay(displayId);              // gone / world-changed → erase
                            cancel(holder[0]);
                            return;
                        }
                        boolean looking = ctx.safety().call("beat.name_on_wall.los",
                                () -> ctx.reveal().hasLineOfSight(watcher, watchAt), Boolean.TRUE);
                        if (!Boolean.TRUE.equals(looking)) {
                            removeDisplay(displayId);              // looked away → the name was never there
                            cancel(holder[0]);
                        }
                    });
        }

        return BeatResult.fired("name-on-wall");
    }

    /* ------------------------------------------------------------------ */
    /*  Placement + cleanup                                                */
    /* ------------------------------------------------------------------ */

    /**
     * Where to hang the name: a point {@code distance} blocks along the player's gaze, at eye height, in
     * a loaded chunk. We do NOT require a solid wall behind it — a self-lit billboard reads fine floating
     * just off a surface — but we keep it in front of the player so it lands in their view as they turn.
     * Returns null if unresolvable (no target / unloaded). MAIN thread.
     */
    private static Location placement(BeatRequest req) {
        if (!req.hasTarget()) return null;
        Player pl = req.targetPlayer();
        int distance = Math.max(2, Math.min(8, req.payload().integer("distance", 3)));
        Location eye = pl.getEyeLocation();
        if (eye.getWorld() == null) return null;
        org.bukkit.util.Vector dir = eye.getDirection().clone();
        dir.setY(dir.getY() * 0.3);                  // flatten gaze so the name sits near eye level, not on the floor
        if (dir.lengthSquared() < 1.0e-6) return null;
        dir.normalize();
        Location spot = eye.clone().add(dir.multiply(distance));
        if (!spot.getWorld().isChunkLoaded(spot.getBlockX() >> 4, spot.getBlockZ() >> 4)) return null;
        // Face the display back toward the player (matters only when billboard is FIXED).
        org.bukkit.util.Vector back = pl.getLocation().toVector().subtract(spot.toVector());
        if (back.lengthSquared() > 1.0e-6) spot.setDirection(back);
        return spot;
    }

    /** Remove the display by id, re-resolving it each time (safe if it already vanished). MAIN thread. */
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
        return s.length() > 96 ? s.substring(0, 96) : s;
    }

    /** Parse a colour: hex ("#rrggbb" / "rrggbb") or a named Adventure colour; blood-dark default. */
    static TextColor colorOf(String name) {
        if (name == null || name.isBlank()) return DEFAULT_COLOR;
        String n = name.trim();
        try {
            if (n.startsWith("#")) {
                TextColor c = TextColor.fromHexString(n);
                if (c != null) return c;
            }
            if (n.matches("[0-9a-fA-F]{6}")) {
                TextColor c = TextColor.fromHexString("#" + n);
                if (c != null) return c;
            }
            net.kyori.adventure.text.format.NamedTextColor named =
                    net.kyori.adventure.text.format.NamedTextColor.NAMES.value(
                            n.toLowerCase(java.util.Locale.ROOT));
            if (named != null) return named;
        } catch (Throwable ignored) { }
        return DEFAULT_COLOR;
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (server-free; mirrors the repo idiom).  */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the pure helpers the scare leans on: hex + named colour parse (and fall back to the dark
     * default on junk, never null), and line clamping bounds a malformed payload. A regression here
     * would either NPE the display text/colour or let an unbounded name through onto the wall.
     */
    static boolean nameOnWallSelfTest() {
        if (colorOf("#8a1c1c") == null) return false;
        if (colorOf("8a1c1c") == null) return false;
        if (colorOf("red") != net.kyori.adventure.text.format.NamedTextColor.RED) return false;
        if (colorOf("not-a-color") != DEFAULT_COLOR) return false;
        if (colorOf(null) != DEFAULT_COLOR) return false;
        if (clamp(null).length() != 0) return false;
        return clamp("x".repeat(200)).length() == 96;
    }
}
