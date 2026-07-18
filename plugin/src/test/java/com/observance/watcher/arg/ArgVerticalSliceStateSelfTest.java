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
            expect(state.submitTheory("", "a") == ArgVerticalSliceState.TheoryResult.INCOMPLETE, "empty is incomplete");
            expect(state.submitTheory("the register clock was wrong", "a") == ArgVerticalSliceState.TheoryResult.WRONG, "wrong theory rejected");
            expect(!state.theoryEarned() && state.receipts().isEmpty(), "wrong answer changes no state");
            expect(state.submitTheory("Refuge before rite; safety became obedience.", "shared-answer")
                    == ArgVerticalSliceState.TheoryResult.ACCEPTED, "correct shared answer accepted with zero observations");
            expect(state.theoryEarned() && state.receipts().size() == 1, "theory creates one response event");
            state.submitTheory("REFUGE BEFORE RITE SAFETY BECAME OBEDIENCE", "different-player");
            expect(state.receipts().size() == 1, "same accepted meaning is idempotent across players");
            expect(state.selectServiceCards("b") == ArgVerticalSliceState.SelectionResult.ACCEPTED, "service curation accepted");
            expect(!state.commitCuration("b"), "one half cannot commit curation");
            expect(state.selectPenaltyCustody("c") == ArgVerticalSliceState.SelectionResult.ACCEPTED, "penalty custody accepted");
            expect(state.commitCuration("c") && state.curated(), "two physical choices commit P5 response");
            int receipts = state.receipts().size();
            expect(state.commitCuration("late-player") && state.receipts().size() == receipts, "curation replay idempotent");

            ArgVerticalSliceState restarted = ArgVerticalSliceState.open(journal);
            expect(restarted.theoryEarned() && restarted.serviceCardsPublic()
                    && restarted.penaltyCopiesInCustody() && restarted.curated(), "restart restores exact state");
            expect(restarted.submitTheory("the hold sheltered families before safety became control", "external-shared")
                    == ArgVerticalSliceState.TheoryResult.ACCEPTED, "accepted alias needs no source receipts after restart");
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
