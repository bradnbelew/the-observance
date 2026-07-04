package com.observance.watcher.util;

import java.util.List;

/**
 * Parity/behavior guard for {@link TextFit} — proves the real-display-limit wrapping actually holds
 * (word-preserving, width-respecting, terminates on pathological input). Dependency-free +
 * main()-runnable, so it compiles and runs with javac alone (no Paper/gson/JUnit on the classpath):
 *
 *   javac -d out plugin/.../util/TextFit.java plugin/.../util/TextFitSelfTest.java
 *   java  -cp out com.observance.watcher.util.TextFitSelfTest
 *
 * Exits non-zero on any failed assertion.
 */
public final class TextFitSelfTest {

    private static int failures = 0;

    private static void check(String label, boolean cond) {
        if (cond) {
            System.out.println("  ok   " + label);
        } else {
            failures++;
            System.out.println("  FAIL " + label);
        }
    }

    public static void main(String[] args) {
        check("TextFit.selfTest() passes its own bundled assertions", TextFit.selfTest());

        // The exact real regression this class exists to prevent: a ~430-char authored line (like
        // voice.ts's keeperPageHeading_brann) must come back as MULTIPLE real, in-width book pages,
        // never one oversized page a vanilla client would silently clip.
        String brann =
                "under brann. the one called NAME keeps the watch, keeps the watch, on the black moon; "
                        + "brann kept that watch, kept that watch, and knows it when he sees it, and names them "
                        + "by it, and counts the naming twice to be sure, and counts it again, and the count "
                        + "comes out one over the stones every time, in the dark, where he does his counting.";
        check("a ~430-char authored page is longer than one real book page", brann.length() > TextFit.BOOK_PAGE_CHARS);
        List<String> pages = TextFit.paginate(brann);
        check("it becomes MORE than one real page", pages.size() > 1);
        for (String pg : pages) {
            check("every real page fits within BOOK_PAGE_CHARS (" + pg.length() + ")", pg.length() <= TextFit.BOOK_PAGE_CHARS);
        }
        StringBuilder rebuilt = new StringBuilder();
        for (String pg : pages) rebuilt.append(rebuilt.length() == 0 ? "" : " ").append(pg);
        check("no word was lost or reordered across the split", rebuilt.toString().equals(brann));

        // A real cipher plaintext (short) must NOT get needlessly split into a second page.
        check("a short cipher line ('GIVE THE FIRST...') stays on ONE page",
                TextFit.paginate("GIVE THE FIRST OF THE DEEP BACK TO THE DEEP").size() == 1);

        // A sign genuinely cannot show more than 4 lines of ~15 chars — verify the real vanilla ceiling.
        check("SIGN_LINE_CHARS matches vanilla's own sign-editing limit", TextFit.SIGN_LINE_CHARS == 15);
        check("a sign has exactly 4 lines", TextFit.SIGN_LINES == 4);
        List<String> signLines = TextFit.wrapForSign("no wall was ever built here");
        check("sign wrap never exceeds 4 lines", signLines.size() <= 4);
        for (String ln : signLines) check("every sign line fits SIGN_LINE_CHARS (" + ln.length() + ")", ln.length() <= TextFit.SIGN_LINE_CHARS);

        if (failures > 0) {
            System.out.println("\nTextFitSelfTest: " + failures + " FAILED");
            System.exit(1);
        }
        System.out.println("\nTextFitSelfTest: OK — real Minecraft display limits are actually enforced.");
    }
}
