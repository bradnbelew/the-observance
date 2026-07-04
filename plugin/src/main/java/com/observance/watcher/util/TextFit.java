package com.observance.watcher.util;

import java.util.ArrayList;
import java.util.List;

/**
 * TextFit — word-wrap and pagination sized to REAL vanilla Minecraft display limits, not just a
 * data-safety clamp against a giant string. A book/sign/boss-bar's underlying NBT can hold far more
 * text than the client can actually SHOW: a written/lectern book page does not scroll and is never
 * auto-paginated by the client (~14 lines x ~19 chars at the default font = roughly 240 visible
 * characters before the rest is simply never rendered); a sign has exactly 4 fixed lines, and the
 * vanilla sign-editing screen itself enforces 15 characters per line as the legible limit; a boss bar
 * / action bar is ONE non-wrapping line across the screen, cut off by screen width past a few dozen
 * characters; an item name/lore line is also one non-wrapping tooltip line.
 *
 * <p>Every one of these overflow cases is SILENT — no exception, no server-side error, the excess text
 * is simply absent or clipped on the client. A beat that only clamps against a generous byte-safety
 * ceiling (e.g. 1024 chars for a book page) can pass a build-time check and a runtime check and still
 * hand a player a book whose sentence stops mid-word. This class is the single place that models the
 * REAL client-facing limits, so a beat can convert authored prose into what the client can actually
 * display, rather than trusting that authored text already happens to be short enough.
 */
public final class TextFit {

    private TextFit() {}

    /** Practical single-page visible capacity for a vanilla written/lectern book. Conservative: real
     *  capacity is roughly 250-270 chars at the default font; this leaves margin for a narrower or
     *  wider custom resource-pack font. */
    public static final int BOOK_PAGE_CHARS = 240;

    /** Vanilla WRITTEN_BOOK hard page-count ceiling (the game itself won't hold more). */
    public static final int BOOK_MAX_PAGES = 100;

    /** A real sign's actual legible per-line capacity — matches the vanilla sign-editing screen's own
     *  15-character-per-line input limit. */
    public static final int SIGN_LINE_CHARS = 15;

    /** A sign has exactly 4 lines (per side). No fifth line exists to overflow onto. */
    public static final int SIGN_LINES = 4;

    /** Practical single-line capacity for a non-wrapping HUD element (boss bar / action bar) before
     *  it runs off a typical client's screen width. Neither ever wraps to a second line. */
    public static final int HUD_LINE_CHARS = 64;

    /** Practical single-line capacity for an item's display name or one lore line. Neither wraps —
     *  an over-long line just widens the tooltip box absurdly rather than clipping, but this keeps
     *  tooltips reading like the rest of the game's items. */
    public static final int TOOLTIP_LINE_CHARS = 50;

    /**
     * Word-wrap {@code text} into a list of book pages, each within {@link #BOOK_PAGE_CHARS}. Splits
     * ONLY on whitespace (never mid-word) so no page ends on a broken word; a single word longer than
     * a whole page is hard-split (pathological input only — never real authored prose). Blank/empty
     * input yields one empty page (so a book beat never ends up with zero pages for one authored line).
     * Caps at {@link #BOOK_MAX_PAGES} (the vanilla ceiling) — authored text should never approach this.
     */
    public static List<String> paginate(String text) {
        return paginate(text, BOOK_PAGE_CHARS);
    }

    public static List<String> paginate(String text, int maxCharsPerPage) {
        List<String> pages = new ArrayList<>();
        if (text == null || text.isBlank()) {
            pages.add("");
            return pages;
        }
        StringBuilder page = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            if (pages.size() >= BOOK_MAX_PAGES) break;
            // A single pathological word longer than a whole page: hard-split rather than loop forever.
            if (word.length() > maxCharsPerPage) {
                if (page.length() > 0) { pages.add(page.toString()); page.setLength(0); }
                int i = 0;
                while (i < word.length() && pages.size() < BOOK_MAX_PAGES) {
                    int end = Math.min(word.length(), i + maxCharsPerPage);
                    pages.add(word.substring(i, end));
                    i = end;
                }
                continue;
            }
            int extra = page.length() == 0 ? word.length() : word.length() + 1;
            if (page.length() + extra > maxCharsPerPage) {
                pages.add(page.toString());
                page.setLength(0);
            }
            if (page.length() > 0) page.append(' ');
            page.append(word);
        }
        if (page.length() > 0 && pages.size() < BOOK_MAX_PAGES) pages.add(page.toString());
        if (pages.isEmpty()) pages.add("");
        return pages;
    }

    /**
     * Word-wrap {@code text} into up to {@link #SIGN_LINES} lines of {@link #SIGN_LINE_CHARS} each —
     * a real sign's actual legible capacity. Overflow beyond 4 lines is DROPPED (a sign physically
     * cannot show a 5th line); authored flavor text meant for a sign should stay short rather than
     * lean on this to rescue long prose.
     */
    public static List<String> wrapForSign(String text) {
        return wrapForSign(text, SIGN_LINE_CHARS, SIGN_LINES);
    }

    public static List<String> wrapForSign(String text, int maxLineChars, int maxLines) {
        List<String> lines = paginate(text, maxLineChars);
        if (lines.size() > maxLines) lines = new ArrayList<>(lines.subList(0, maxLines));
        return lines;
    }

    /** Hard single-line clamp for a non-wrapping HUD element (boss bar / action bar) or an item
     *  name/lore line. Never wraps — there is nowhere for a second line to go on these surfaces —
     *  just truncates at a realistic on-screen width instead of an arbitrary byte-safety ceiling. */
    public static String clampLine(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (mirrors the repo's selftest idiom).    */
    /* ------------------------------------------------------------------ */

    public static boolean selfTest() {
        // paginate: blank -> one empty page; never re-orders or drops words; every page within width.
        List<String> p1 = paginate("", 10);
        if (p1.size() != 1 || !p1.get(0).isEmpty()) return false;

        String source = "one two three four five six seven eight nine ten";
        List<String> p2 = paginate(source, 12);
        for (String pg : p2) if (pg.length() > 12) return false;
        StringBuilder rebuilt = new StringBuilder();
        for (String pg : p2) rebuilt.append(rebuilt.length() == 0 ? "" : " ").append(pg);
        if (!rebuilt.toString().equals(source)) return false;

        // a pathological single overlong word still terminates and hard-splits rather than looping.
        List<String> p3 = paginate("aaaaaaaaaaaaaaaaaaaa", 5); // 20 a's, width 5
        if (p3.size() != 4) return false;
        for (String pg : p3) if (pg.length() != 5) return false;

        // wrapForSign: caps at 4 lines even when the source would need far more.
        List<String> s1 = wrapForSign("a b c d e f g h i j k l m n o p", 1, 4);
        if (s1.size() != 4) return false;

        // clampLine: no truncation under the limit, a hard cut at the limit, never throws on null.
        if (!clampLine("short", 10).equals("short")) return false;
        if (clampLine("x".repeat(20), 10).length() != 10) return false;
        if (!clampLine(null, 10).isEmpty()) return false;

        return true;
    }
}
