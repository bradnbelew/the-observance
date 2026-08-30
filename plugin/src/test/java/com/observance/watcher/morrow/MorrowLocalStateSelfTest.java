package com.observance.watcher.morrow;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MorrowLocalStateSelfTest {
    private MorrowLocalStateSelfTest() { }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("morrow-local-");
        Path journal = directory.resolve("campaign.journal");
        try {
            MorrowLocalState state = MorrowLocalState.open(journal, "morrow.rehearsal.001");
            check(state.snapshot().stage() == MorrowStage.HELPFUL, "initial state");
            check(state.pendingAfter(0).isEmpty(), "initialization is not projected");

            expectState(() -> state.commit("morrow.act1.room04_witnessed", "paper:room04:early:001", bytes("{}")));
            state.acceptProjection("morrow.act0.case_chain_authenticated", "copperline:case:001", bytes("{}"));
            state.acceptProjection("morrow.act0.server_handoff_recovered", "copperline:handoff:001", bytes("{}"));
            state.commit("morrow.act1.room04_witnessed", "paper:room04:witness:001", bytes("{}"));

            byte[] firstPayload = "{\"proposal\":\"sixth-block\"}".getBytes(StandardCharsets.UTF_8);
            MorrowLocalState.CommitResult first = state.commit(
                    "morrow.act1.static_proposal_authenticated", "paper:room04:proposal:001", firstPayload);
            check(first.created(), "first event is durable");
            check(state.snapshot().stage() == MorrowStage.HELPFUL, "one proof cannot advance");
            MorrowLocalState.CommitResult duplicate = state.commit(
                    "morrow.act1.static_proposal_authenticated", "paper:room04:proposal:001", firstPayload);
            check(!duplicate.created() && duplicate.sequence() == first.sequence(), "duplicate is idempotent");

            state.commit("morrow.act1.intention_error_proven", "paper:room04:error:001", bytes("{}"));
            check(state.snapshot().stage() == MorrowStage.HELPFUL, "proofs cannot substitute for consent");
            state.commit("morrow.act1.entity_replay_authorized", "paper:room04:authorize:001", bytes("{\"option\":\"authorize\"}"));
            check(state.snapshot().stage() == MorrowStage.CURIOUS, "complete proof advances one state");
            check(state.pendingAfter(0).size() == 4, "only locally owned offline events await projection");

            MorrowLocalState restarted = MorrowLocalState.open(journal, "morrow.rehearsal.001");
            check(restarted.snapshot().stage() == MorrowStage.CURIOUS, "restart reconstructs projection");
            expectIllegal(() -> restarted.commit("morrow.act0.case_chain_authenticated", "paper:web-owned:001", bytes("{}")));
            expectIllegal(() -> restarted.acceptProjection("morrow.act1.room04_witnessed", "remote:mc-owned:001", bytes("{}")));
            expectState(() -> restarted.commit(
                    "morrow.act1.static_proposal_authenticated", "paper:room04:proposal:001", bytes("{\"changed\":true}")));
            expectIo(() -> MorrowLocalState.open(journal, "morrow.production.001"));
            System.out.println("MORROW LOCAL STATE: PASS");
        } finally {
            Files.deleteIfExists(journal);
            Files.deleteIfExists(directory);
        }
    }

    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void expectIllegal(Throwing action) throws Exception {
        try { action.run(); throw new AssertionError("expected IllegalArgumentException"); }
        catch (IllegalArgumentException expected) { }
    }
    private static void expectState(Throwing action) throws Exception {
        try { action.run(); throw new AssertionError("expected IllegalStateException"); }
        catch (IllegalStateException expected) { }
    }
    private static void expectIo(Throwing action) throws Exception {
        try { action.run(); throw new AssertionError("expected IOException"); }
        catch (java.io.IOException expected) { }
    }
    @FunctionalInterface private interface Throwing { void run() throws Exception; }
}
