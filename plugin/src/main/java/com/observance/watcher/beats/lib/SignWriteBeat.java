package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.Placement;
import com.observance.watcher.util.TextFit;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;

import java.util.List;

/**
 * TEXT — a sign writes or rewrites itself out of sight. If a sign exists at the anchor its lines are
 * replaced; otherwise (when authorized) a free-standing sign is placed on valid support. All four
 * lines come from the payload. The mutation is reveal-disciplined (discovered, never witnessed).
 *
 * <p>Payload:
 * <pre>{@code
 * { "lines":[ "l1","l2","l3","l4" ], "side":"front",
 *   "place_if_missing":false, "material":"OAK_SIGN", "glowing":false }
 * }</pre>
 */
public final class SignWriteBeat extends AbstractBeat {

    @Override public String name() { return "sign_write"; }
    @Override public String description() { return "A sign writes or rewrites itself out of sight."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (req.payload().stringList("lines").isEmpty()) return false;
        Location a = anchor(ctx, req);
        if (a == null || a.getWorld() == null) return false;
        if (!a.getWorld().isChunkLoaded(a.getBlockX() >> 4, a.getBlockZ() >> 4)) return false;
        Block b = a.getBlock();
        boolean isSign = b.getState() instanceof Sign;
        return isSign || req.payload().bool("place_if_missing", false);
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Location a = anchor(ctx, req);
        if (a == null) return BeatResult.skipped("no-anchor");
        BeatPayload p = req.payload();
        List<String> lines = p.stringList("lines");
        if (lines.isEmpty()) return BeatResult.skipped("no-lines");

        Block block = a.getBlock();
        boolean existing = block.getState() instanceof Sign;
        if (!existing) {
            if (!p.bool("place_if_missing", false)) return BeatResult.skipped("no-sign");
            if (!Placement.canStandOn(block)) return BeatResult.skipped("no-support");
        }

        final Material signMat = material(p.string("material", "OAK_SIGN"), Material.OAK_SIGN);
        final Side side = "back".equalsIgnoreCase(p.string("side", "front")) ? Side.BACK : Side.FRONT;
        final boolean glowing = p.bool("glowing", false);

        mutateWhenUnwitnessed(ctx, block, () -> {
            Block target = a.getBlock();
            if (!(target.getState() instanceof Sign)) {
                if (!signMat.name().contains("SIGN")) {
                    target.setType(Material.OAK_SIGN, false);
                } else {
                    target.setType(signMat, false);
                }
                ctx.protectedRegistry().protect(target);
            }
            if (!(target.getState() instanceof Sign sign)) return;
            var sideText = sign.getSide(side);
            for (int i = 0; i < 4; i++) {
                String text = i < lines.size() && lines.get(i) != null ? lines.get(i) : "";
                sideText.line(i, Component.text(clampLine(text)));
            }
            try { sideText.setGlowingText(glowing); } catch (Throwable ignored) { }
            sign.setWaxed(true);  // anti-grief: waxed signs can't be edited by players
            sign.update(true, false);
        });
        return BeatResult.fired("sign-written");
    }

    // A real sign has ~15 legible characters per line (the vanilla sign-editing screen enforces
    // exactly this) — not the far more generous data-safety ceiling this used to clamp against.
    // Authored "lines" are pre-composed per-slot text (not one flowing string), so this does not
    // reflow overflow onto a neighboring line — it makes the actual displayable limit visible at the
    // point authored content is written, instead of silently accepting text a real sign cannot show.
    private static String clampLine(String s) {
        return TextFit.clampLine(s, TextFit.SIGN_LINE_CHARS);
    }
}
