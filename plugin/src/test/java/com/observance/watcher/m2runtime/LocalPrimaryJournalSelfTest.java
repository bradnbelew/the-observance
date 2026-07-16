package com.observance.watcher.m2runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Restart, idempotency, catch-up, and corruption tests for local-primary state. */
public final class LocalPrimaryJournalSelfTest {
    private LocalPrimaryJournalSelfTest() { }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("observance-m2-journal-");
        Path journalPath = dir.resolve("campaign.journal");
        try {
            LocalPrimaryJournal journal = LocalPrimaryJournal.open(journalPath);
            LocalPrimaryJournal.Receipt first = journal.append("finding:g1:f1", "finding_committed", bytes("one"));
            LocalPrimaryJournal.Receipt duplicate = journal.append("finding:g1:f1", "finding_committed", bytes("one"));
            check(first.eventHash().equals(duplicate.eventHash()), "duplicate returns original receipt");
            check(journal.after(0).size() == 1, "duplicate does not append");
            expectIllegalState(() -> journal.append("finding:g1:f1", "finding_committed", bytes("changed")));
            expectIllegalArgument(() -> journal.append("bad\tkey", "finding_committed", bytes("x")));

            LocalPrimaryJournal restarted = LocalPrimaryJournal.open(journalPath);
            restarted.append("choice:g1:wren", "choice_committed", bytes("understand"));
            check(restarted.after(1).size() == 1, "catch-up cursor returns only missing receipt");
            check(LocalPrimaryJournal.open(journalPath).after(0).size() == 2, "restart retains complete chain");

            String valid = Files.readString(journalPath, StandardCharsets.UTF_8);
            int previousHashOffset = valid.indexOf('\t') + 1;
            char replacement = valid.charAt(previousHashOffset) == 'a' ? 'b' : 'a';
            String corrupted = valid.substring(0, previousHashOffset) + replacement
                    + valid.substring(previousHashOffset + 1);
            Files.writeString(journalPath, corrupted, StandardCharsets.UTF_8);
            expectIo(() -> LocalPrimaryJournal.open(journalPath));
            System.out.println("M2 local-primary journal self-test passed");
        } finally {
            Files.deleteIfExists(journalPath);
            Files.deleteIfExists(dir);
        }
    }

    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void expectIllegalState(Throwing action) throws Exception {
        try { action.run(); throw new AssertionError("expected IllegalStateException"); }
        catch (IllegalStateException expected) { }
    }
    private static void expectIo(Throwing action) throws Exception {
        try { action.run(); throw new AssertionError("expected IOException"); }
        catch (IOException expected) { }
    }
    private static void expectIllegalArgument(Throwing action) throws Exception {
        try { action.run(); throw new AssertionError("expected IllegalArgumentException"); }
        catch (IllegalArgumentException expected) { }
    }
    @FunctionalInterface private interface Throwing { void run() throws Exception; }
}
