package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * NPC DIALOGUE — the Presiding Keeper speaks his bound lines to ONE player, privately, on the
 * rite-side (the Threshold / the Undercroft altar). This is the plugin half of the KeeperNpcBeat tree
 * authored in {@code design/content/npc-dialogue.md §7}: the showrunner resolves which {@code node_key}
 * to open (branching on the dossier — {@code arc_state.flags} + {@code punishment_state}) and the
 * <b>already-resolved text lines</b> for that node, then enqueues this beat with them in the payload.
 *
 * <p><b>No story in the engine (INV-1, the voice rule).</b> This beat hardcodes NOT ONE word of
 * dialogue. Every line is read verbatim from {@code payload.lines} — the bound output of the
 * {@code npcVoice}/{@code keeper.*} registry, exactly as {@link SignWriteBeat} and
 * {@link LecternFillBeat} read their carved text from the payload. The Keeper's register discipline
 * (lowercase, no contraction, second-person-to-the-group, names no one — INV-16; never states
 * FACT 15) is the LORE author's contract on the text it is handed; this class only delivers it.
 *
 * <p><b>Why private chat, not a billboard.</b> The presider addresses "all of you" but each player
 * receives the lines on their own client (deniable, per-player, reveal-trivially-safe — chat is not a
 * world mutation). A group standing together each sees the Keeper speak; nobody witnesses a block or
 * entity change, so the reveal contract is satisfied by construction. The lines drip one per short
 * delay so a wall of text never dumps at once (the cadence reads as speech, not a sign).
 *
 * <p><b>The dossier branch is upstream.</b> Per the design (§7.0), the conduct skin and which node
 * opens are computed by the showrunner from state the engine ALREADY keeps — this beat adds no
 * measurement and makes no branch decision. It receives {@code node_key} (for idempotency / logging
 * only) and the resolved {@code lines}. A re-fire of the same {@code beatId} is suppressed by
 * {@link AbstractBeat}'s idempotency guard, so a node never double-speaks.
 *
 * <p><b>Rune line (optional).</b> A node may carry a {@code rune_line} — a carved-glyph coda in the
 * keepers' alphabet (e.g. a half-veiled cross-surface seed). It renders in the {@code observance:runes}
 * font ONLY when the target's client has applied the resource pack (the showrunner sets
 * {@code pack_loaded} from {@link com.observance.watcher.signal.ResourcePackTracker}); otherwise it is
 * dropped (never tofu boxes). The plain {@code lines} always deliver regardless of pack state.
 *
 * <p>Payload:
 * <pre>{@code
 * {
 *   "node_key": "keeper.seventhChoice.offer",
 *   "speaker":  "the keeper",          // optional name prefix; omit for an unattributed voice
 *   "lines":    [ "...", "..." ],       // the bound keeper.* text, ALREADY resolved by the showrunner
 *   "rune_line": "KEPT NOT YET",        // optional carved-glyph coda (rune font, pack-gated)
 *   "pack_loaded": true,                // showrunner-supplied: is the target's pack applied?
 *   "line_delay_ticks": 35,             // cadence between spoken lines (default 35 = ~1.75s)
 *   "color": "gray"                     // optional NamedTextColor for the spoken lines
 * }
 * }</pre>
 *
 * <p>This beat is {@link BeatCategory#DIRECTED}: it fires in response to a player walking up to the
 * keeper NPC (a deliberate interaction), not on the ambient/personalized drama budget.
 */
public final class KeeperNpcBeat extends AbstractBeat {

    /** The resource-pack font key carrying the keepers' rune alphabet (see resourcepack/README.md). */
    private static final String RUNE_FONT = "observance:runes";

    /** Default cadence between spoken lines — slow enough to read as speech, not a dumped sign. */
    private static final int DEFAULT_LINE_DELAY_TICKS = 35;

    /** A chat line longer than this is clamped (defense against a malformed payload). */
    private static final int MAX_LINE_LEN = 256;

    @Override public String name() { return "keeper_npc"; }
    @Override public String description() { return "The presiding Keeper speaks a resolved dialogue node to one player."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (!req.hasTarget()) return false;
        // Plausible only if there is at least one spoken line OR a rune coda to deliver.
        BeatPayload p = req.payload();
        return !p.stringList("lines").isEmpty() || p.has("rune_line");
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Player pl = target(req);
        if (pl == null) return BeatResult.skipped("no-target");

        BeatPayload p = req.payload();
        List<String> lines = p.stringList("lines");
        String runeLine = p.has("rune_line") ? p.string("rune_line", "") : null;
        if (lines.isEmpty() && (runeLine == null || runeLine.isBlank())) {
            return BeatResult.skipped("no-lines");
        }

        final String speaker = p.string("speaker", null);
        final NamedTextColor color = colorOf(p.string("color", "gray"));
        final int delay = clampDelay(p.integer("line_delay_ticks", DEFAULT_LINE_DELAY_TICKS));
        final boolean packLoaded = p.bool("pack_loaded", false);
        final java.util.UUID id = req.targetUuid();

        // Optional one-time attribution line ("the keeper") before the speech, in the same hushed color.
        int tick = 0;
        if (speaker != null && !speaker.isBlank()) {
            final String who = clamp(speaker);
            scheduleLine(ctx, id, tick, () -> Component.text(who, color));
            tick += delay;
        }

        // Drip each resolved line on the cadence. Re-resolve the player by UUID at each fire so a
        // logout mid-speech simply ends the dialogue (no NPE, no half-delivered jank).
        for (String raw : lines) {
            if (raw == null) continue;
            final String text = clamp(raw);
            if (text.isBlank()) continue;
            scheduleLine(ctx, id, tick, () -> Component.text(text, color));
            tick += delay;
        }

        // The carved-glyph coda last, in the rune font — ONLY if the client can render it.
        if (runeLine != null && !runeLine.isBlank()) {
            if (packLoaded) {
                final String glyphs = clamp(runeLine);
                scheduleLine(ctx, id, tick, () ->
                        Component.text(glyphs, color).font(net.kyori.adventure.key.Key.key(RUNE_FONT)));
            } else if (ctx.config().debug()) {
                ctx.safety().info("beat.keeper",
                        "dropping rune_line for " + pl.getName() + " (pack not loaded) — plain lines still sent");
            }
        }

        return BeatResult.fired("keeper-spoke");
    }

    /* ------------------------------------------------------------------ */
    /*  Delivery                                                           */
    /* ------------------------------------------------------------------ */

    /** Schedule one chat line to the target after {@code delayTicks}, re-resolving the player by UUID. */
    private static void scheduleLine(BeatContext ctx, java.util.UUID id, int delayTicks,
                                     java.util.function.Supplier<Component> lineSupplier) {
        ctx.scheduler().runLaterSafe("beat.keeper.line", Math.max(0, delayTicks), () -> {
            Player p = org.bukkit.Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) return;     // left the scene → dialogue just ends
            Component c = lineSupplier.get();
            if (c != null) p.sendMessage(c);
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static int clampDelay(int t) {
        // 0 (all at once) is legal but discouraged; cap at 10s so a typo can't freeze the conversation.
        return Math.max(0, Math.min(20 * 10, t));
    }

    private static String clamp(String s) {
        if (s == null) return "";
        return s.length() > MAX_LINE_LEN ? s.substring(0, MAX_LINE_LEN) : s;
    }

    /** Parse a NamedTextColor by name (the hushed default is gray). Never throws. */
    static NamedTextColor colorOf(String name) {
        if (name == null || name.isBlank()) return NamedTextColor.GRAY;
        NamedTextColor c = NamedTextColor.NAMES.value(name.trim().toLowerCase(java.util.Locale.ROOT));
        return c == null ? NamedTextColor.GRAY : c;
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (mirrors the repo's selftest idiom).    */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the two pure helpers the beat's correctness leans on: color parsing falls back to gray on
     * junk (never null, never throw), and line clamping bounds a malformed payload. A regression here
     * would either NPE the color or let an unbounded line through to chat.
     */
    static boolean keeperSelfTest() {
        if (colorOf("gray") != NamedTextColor.GRAY) return false;
        if (colorOf("GOLD") != NamedTextColor.GOLD) return false;     // case-insensitive
        if (colorOf("not-a-color") != NamedTextColor.GRAY) return false;
        if (colorOf(null) != NamedTextColor.GRAY) return false;
        if (clamp(null).length() != 0) return false;
        String long_ = "x".repeat(MAX_LINE_LEN + 50);
        return clamp(long_).length() == MAX_LINE_LEN;
    }
}
