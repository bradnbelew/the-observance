package com.observance.watcher.arg;

import com.observance.watcher.m3runtime.BookPageLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pure authored evidence used by both the Paper world and its render/index audit. */
public final class ArgVerticalSliceEvidence {
    public static final List<String> READ_EXTRACT_PAGES = List.of(
            "Billing host steady\nQueue ran normally\nNo restart entered",
            "Offset held within one second BEFORE billing sync\nService 1174 closed\nNode stayed online",
            "Cartridge zero three contains COPY corrected\nRead ended 00:12:08\nBarcode agrees",
            "Cartridge zero four mounted later\nRead ended 00:27:03\nBarcode agrees",
            "The correction appears on both\nInk order differs\nClock offset does not",
            "Guest entry follows bell\nArchive copy is earlier\nSOURCE remains cartridge zero four"
    );
    public static final List<Index> READ_EXTRACT_INDEX = List.of(
            new Index(3, 1, 5),
            new Index(2, 1, 6),
            new Index(6, 3, 1)
    );
    public static final String READ_EXTRACT_RESULT = "COPY BEFORE SOURCE";

    private ArgVerticalSliceEvidence() { }

    public static String bookBody() {
        return String.join("\f", READ_EXTRACT_PAGES);
    }

    public static String decodeReadExtract() {
        List<String> words = new ArrayList<>();
        for (Index index : READ_EXTRACT_INDEX) {
            String[] lines = READ_EXTRACT_PAGES.get(index.page() - 1).split("\\n", -1);
            String[] lineWords = lines[index.line() - 1].trim().split("\\s+");
            words.add(lineWords[index.word() - 1].replaceAll("[^A-Za-z0-9]", "")
                    .toUpperCase(Locale.ROOT));
        }
        return String.join(" ", words);
    }

    public static List<BookPageLayout.PageBudget> pageBudgets() {
        return READ_EXTRACT_PAGES.stream().map(BookPageLayout::measure).toList();
    }

    public record Index(int page, int line, int word) { }
}
