package com.observance.watcher.m3runtime;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Conservative Minecraft 1.21.11 written-book layout authority for filing prompts and legacy clauses. */
public final class BookPageLayout {
    public static final int PAGE_PIXEL_WIDTH = 114;
    public static final int LINE_HEIGHT_PIXELS = 9;
    public static final int MAX_RENDERED_LINES = 13;

    private BookPageLayout() { }

    public static EntryPage entryPage(String heading, String finding, String question, boolean filed) {
        String status = filed ? "ON FILE" : "OPEN";
        String visibleText = heading + "\n" + status + "\n\n" + question + "\n\n"
                + (filed ? "Finding retained." : "BEGIN ENTRY");
        PageBudget budget = measure(visibleText);
        if (!budget.fits()) {
            throw new IllegalArgumentException("filing prompt exceeds written-book render budget: "
                    + finding + " lines=" + budget.renderedLines() + " width=" + budget.maximumLinePixels());
        }
        String command = "/obsfile " + ("P4.F5".equals(finding) ? "conclude " : "file " + finding + " ");
        return new EntryPage(heading, finding, question, filed, visibleText, command, budget);
    }

    public static List<OptionPage> optionPages(String heading, String finding, String selected,
            List<Option> choices) {
        if (choices.size() != 4) throw new IllegalArgumentException("each filing heading requires four clauses");
        List<OptionPage> pages = new ArrayList<>();
        Set<String> commands = new LinkedHashSet<>();
        for (int index = 0; index < choices.size(); index++) {
            Option choice = choices.get(index);
            String command = "/obsfile mark " + finding + " " + choice.id();
            String marker = choice.id().equals(selected) ? "[X] " : "[ ] ";
            String folio = "CLAUSE " + (char) ('A' + index) + " - " + (index + 1) + "/4";
            String visibleText = heading + "\n" + folio + "\n\n" + marker + choice.label()
                    + "\n\nMARK THIS CLAUSE";
            PageBudget budget = measure(visibleText);
            if (!budget.fits()) {
                throw new IllegalArgumentException("filing clause exceeds written-book render budget: "
                        + finding + "/" + choice.id() + " lines=" + budget.renderedLines()
                        + " width=" + budget.maximumLinePixels());
            }
            if (!commands.add(command)) throw new IllegalArgumentException("duplicate filing command: " + command);
            pages.add(new OptionPage(heading, finding, index, choice, marker, folio, visibleText,
                    command, budget));
        }
        return List.copyOf(pages);
    }

    public static PageBudget measure(String text) {
        int lines = 1;
        int maximum = 0;
        String[] paragraphs = text.split("\\n", -1);
        for (int paragraphIndex = 0; paragraphIndex < paragraphs.length; paragraphIndex++) {
            if (paragraphIndex > 0) lines++;
            String paragraph = paragraphs[paragraphIndex];
            int linePixels = 0;
            if (paragraph.isEmpty()) continue;
            String[] words = paragraph.split(" ", -1);
            for (int wordIndex = 0; wordIndex < words.length; wordIndex++) {
                int separator = wordIndex == 0 ? 0 : glyphAdvance(' ');
                int wordPixels = width(words[wordIndex]);
                if (linePixels > 0 && linePixels + separator + wordPixels > PAGE_PIXEL_WIDTH) {
                    maximum = Math.max(maximum, linePixels);
                    lines++;
                    linePixels = 0;
                    separator = 0;
                }
                if (wordPixels <= PAGE_PIXEL_WIDTH) {
                    linePixels += separator + wordPixels;
                    continue;
                }
                for (int i = 0; i < words[wordIndex].length(); i++) {
                    int advance = glyphAdvance(words[wordIndex].charAt(i));
                    if (linePixels + advance > PAGE_PIXEL_WIDTH) {
                        maximum = Math.max(maximum, linePixels);
                        lines++;
                        linePixels = 0;
                    }
                    linePixels += advance;
                }
            }
            maximum = Math.max(maximum, linePixels);
        }
        return new PageBudget(text.length(), lines, maximum,
                lines <= MAX_RENDERED_LINES && maximum <= PAGE_PIXEL_WIDTH);
    }

    private static int width(String text) {
        int pixels = 0;
        for (int index = 0; index < text.length(); index++) pixels += glyphAdvance(text.charAt(index));
        return pixels;
    }

    /* Upper-bounds the vanilla default-font advances used by the ASCII ledger copy. */
    private static int glyphAdvance(char character) {
        if (character == ' ') return 4;
        if (".,:;!'|iIl".indexOf(character) >= 0) return 3;
        if ("[](){}tfr".indexOf(character) >= 0) return 5;
        return 6;
    }

    public static Audit audit(Map<String, List<Option>> findings) {
        List<OptionPage> pages = new ArrayList<>();
        for (Map.Entry<String, List<Option>> row : findings.entrySet()) {
            pages.addAll(optionPages(row.getKey(), row.getKey(), null, row.getValue()));
        }
        Set<String> ids = new LinkedHashSet<>();
        Set<String> commands = new LinkedHashSet<>();
        for (OptionPage page : pages) {
            ids.add(page.finding() + "/" + page.choice().id());
            commands.add(page.command());
        }
        return new Audit(List.copyOf(pages), ids.size(), commands.size(),
                pages.stream().allMatch(page -> page.budget().fits()));
    }

    public record Option(String label, String id) { }
    public record EntryPage(String heading, String finding, String question, boolean filed,
            String visibleText, String command, PageBudget budget) { }
    public record EntryAudit(List<EntryPage> pages, int uniqueFindings, int uniqueCommands,
            boolean allFit) { }
    public record PageBudget(int characters, int renderedLines, int maximumLinePixels, boolean fits) { }
    public record OptionPage(String heading, String finding, int index, Option choice, String marker,
            String folio, String visibleText, String command, PageBudget budget) { }
    public record Audit(List<OptionPage> pages, int uniqueOptions, int uniqueCommands, boolean allFit) { }

    public static EntryAudit entryAudit(List<EntryPage> pages) {
        Set<String> findings = new LinkedHashSet<>();
        Set<String> commands = new LinkedHashSet<>();
        for (EntryPage page : pages) {
            findings.add(page.finding());
            commands.add(page.command());
        }
        return new EntryAudit(List.copyOf(pages), findings.size(), commands.size(),
                pages.stream().allMatch(page -> page.budget().fits()));
    }
}
