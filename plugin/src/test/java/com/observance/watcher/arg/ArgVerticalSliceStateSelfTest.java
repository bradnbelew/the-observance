package com.observance.watcher.arg;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ArgVerticalSliceStateSelfTest {
    private ArgVerticalSliceStateSelfTest() { }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("observance-arg-slice-");
        Path journal = dir.resolve("slice.journal");
        try {
            ArgVerticalSliceState state = ArgVerticalSliceState.open(journal);
            expect(state.submitConclusion("", "", "", "a") == ArgVerticalSliceState.TheoryResult.INCOMPLETE, "empty is incomplete");
            expect(state.submitConclusion("a ritual chamber", "smoke directions became mandatory attendance",
                    "copy before source", "a") == ArgVerticalSliceState.TheoryResult.WRONG, "wrong purpose rejected");
            expect(!state.theoryEarned() && state.receipts().isEmpty(), "wrong answer changes no state");
            expect(state.submitConclusion("a shelter", "safety did not become control", "copy before source", "a")
                    == ArgVerticalSliceState.TheoryResult.WRONG, "contradictory change rejected");
            expect(state.submitConclusion("refuge shelter people workers families records records records records records records records records",
                    "safety rules became mandatory", "copy before source", "a")
                    == ArgVerticalSliceState.TheoryResult.WRONG, "irrelevant keyword stuffing rejected by bounded claim shape");
            expect(state.submitConclusion("an emergency shelter for local households",
                    "smoke guidance was rewritten as compulsory attendance",
                    "the corrected copy predates its recorded original", "shared-answer")
                    == ArgVerticalSliceState.TheoryResult.ACCEPTED, "natural paraphrases accepted with zero observations");
            expect(state.theoryEarned() && state.receipts().size() == 1, "theory creates one response event");
            state.submitConclusion("ordinary refuge", "safety procedures became control",
                    "copy before source", "different-player");
            expect(state.receipts().size() == 1, "same accepted meaning is idempotent across players");
            expect(state.selectServiceCards("b") == ArgVerticalSliceState.SelectionResult.ACCEPTED, "service curation accepted");
            expect(!state.commitCuration("b"), "one half cannot commit curation");
            expect(state.selectPenaltyCustody("c") == ArgVerticalSliceState.SelectionResult.ACCEPTED, "penalty custody accepted");
            expect(state.commitCuration("c") && state.serviceChronologyShared() && state.curated(),
                    "two physical choices commit chronology and P5 response");
            int receipts = state.receipts().size();
            expect(state.commitCuration("late-player") && state.receipts().size() == receipts, "curation replay idempotent");

            ArgVerticalSliceState restarted = ArgVerticalSliceState.open(journal);
            expect(restarted.theoryEarned() && restarted.serviceCardsPublic()
                    && restarted.penaltyCopiesInCustody() && restarted.serviceChronologyShared()
                    && restarted.curated(), "restart restores exact state");
            expect(restarted.submitConclusion("a shelter", "emergency instructions became mandatory rules",
                    "the source came after the copy", "external-shared")
                    == ArgVerticalSliceState.TheoryResult.ACCEPTED, "different wording needs no source receipts after restart");
            expect(restarted.submitConclusion("The Hold sheltered families before safety became control", "external-shared")
                    == ArgVerticalSliceState.TheoryResult.INCOMPLETE, "hidden long canonical sentence is not an input contract");
            System.out.println("ArgVerticalSliceStateSelfTest OK - zero-observation theory, P5 physical curation, restart, idempotency");
        } finally {
            Files.deleteIfExists(journal);
            Files.deleteIfExists(dir);
        }
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
