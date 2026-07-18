package com.observance.watcher.arg;

import com.observance.watcher.m3runtime.BookPageLayout;

/** Exact authored cipher and conservative written-book client budget receipt. */
public final class ArgVerticalSliceEvidenceSelfTest {
    private ArgVerticalSliceEvidenceSelfTest() { }

    public static void main(String[] args) {
        require(ArgVerticalSliceEvidence.READ_EXTRACT_PAGES.size() == 6, "read extract must have six pages");
        require(ArgVerticalSliceEvidence.READ_EXTRACT_INDEX.size() == 3, "index must have three coordinates");
        require(ArgVerticalSliceEvidence.READ_EXTRACT_RESULT.equals(ArgVerticalSliceEvidence.decodeReadExtract()),
                "page-line-word result drifted");
        int page = 0;
        for (BookPageLayout.PageBudget budget : ArgVerticalSliceEvidence.pageBudgets()) {
            page++;
            require(budget.fits(), "read extract page " + page + " exceeds client book budget: " + budget);
            require(budget.renderedLines() <= BookPageLayout.MAX_RENDERED_LINES,
                    "read extract page " + page + " line overflow");
            require(budget.maximumLinePixels() <= BookPageLayout.PAGE_PIXEL_WIDTH,
                    "read extract page " + page + " width overflow");
        }
        require(ArgVerticalSliceEvidence.bookBody().chars().filter(value -> value == '\f').count() == 5,
                "page separators drifted");
        System.out.println("ArgVerticalSliceEvidenceSelfTest OK: pages=6 decode=COPY BEFORE SOURCE");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
