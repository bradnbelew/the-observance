package com.observance.watcher.morrow.dialog;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.MorrowStage;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority.Action;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority.Decision;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority.View;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Main-driven one/two/six-player, Escape, decline, prerequisite, receipt, and replay tests. */
public final class MorrowDialogAuthoritySelfTest {
    private static final String RELEASE = "morrow.rehearsal.001";

    private MorrowDialogAuthoritySelfTest() { }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("morrow-dialog-authority-");
        Path journal = directory.resolve("morrow.journal");
        try {
            MorrowLocalState state = MorrowLocalState.open(journal, RELEASE);
            check(MorrowDialogAuthority.currentView(state.snapshot()) == View.TERMINAL_GREETING,
                    "initial view is terminal greeting");
            Decision early = MorrowDialogAuthority.decide(Action.ACKNOWLEDGE_ROOM, state.snapshot());
            check(early.status() == MorrowDialogAuthority.Status.NOT_READY && !early.commitsReceipt(),
                    "greeting cannot bypass external handoff");
            check(state.pendingAfter(0).isEmpty(), "opening or escaping greeting creates no receipt");

            state.acceptProjection("morrow.act0.case_chain_authenticated", "copperline:case:001", bytes("{}"));
            state.acceptProjection("morrow.act0.server_handoff_recovered", "copperline:handoff:001", bytes("{}"));
            commitAsGroup(state, Action.ACKNOWLEDGE_ROOM, 6);
            check(state.pendingAfter(0).size() == 1, "six greeting callbacks create one room receipt");
            check(MorrowDialogAuthority.currentView(state.snapshot()) == View.RESTORATION_PROPOSAL,
                    "room receipt opens restoration proposal");

            Decision proposal = MorrowDialogAuthority.decide(Action.AUTHENTICATE_PROPOSAL, state.snapshot());
            String proposalPayload = new String(proposal.payload(), StandardCharsets.UTF_8);
            check(proposalPayload.contains("\"world_mutation\":\"bounded_static_restore\"")
                            && proposalPayload.contains("\"bounded_cells\":6")
                            && proposalPayload.contains("\"rollback\":\"six_cell_baseline\""),
                    "proposal receipt binds the exact bounded restoration and rollback");
            commitAsGroup(state, Action.AUTHENTICATE_PROPOSAL, 2);
            check(state.pendingAfter(0).size() == 2, "two proposal callbacks create one proposal receipt");
            check(MorrowDialogAuthority.currentView(state.snapshot()) == View.EVIDENCE_REVIEW,
                    "authenticated proposal opens evidence review");

            int beforeReview = state.pendingAfter(0).size();
            for (int player = 0; player < 6; player++) {
                Decision review = MorrowDialogAuthority.decide(Action.REVIEW_EVIDENCE, state.snapshot());
                check(!review.commitsReceipt(), "evidence review is read-only");
            }
            check(state.pendingAfter(0).size() == beforeReview,
                    "review and Escape-safe close create zero receipts");
            Decision prematureAuthorization = MorrowDialogAuthority.decide(
                    Action.AUTHORIZE_ENTITY_REPLAY, state.snapshot());
            check(prematureAuthorization.status() == MorrowDialogAuthority.Status.NOT_READY
                            && !prematureAuthorization.commitsReceipt(),
                    "Entity Replay cannot bypass sixth-block proof");

            state.commit(MorrowDialogAuthority.INTENTION_ERROR_PROVEN,
                    "paper:room04:intention-error:selftest", bytes("{\"classification\":\"inferred\"}"));
            check(MorrowDialogAuthority.currentView(state.snapshot()) == View.ENTITY_REPLAY_AUTHORIZATION,
                    "proven intention error offers explicit authorization");
            int beforeDecline = state.pendingAfter(0).size();
            for (int player = 0; player < 6; player++) {
                Decision decline = MorrowDialogAuthority.decide(Action.DECLINE_ENTITY_REPLAY, state.snapshot());
                check(!decline.commitsReceipt(), "decline creates no authorization receipt");
            }
            check(state.pendingAfter(0).size() == beforeDecline,
                    "six declines preserve relationship and journal");

            Decision authorization = MorrowDialogAuthority.decide(
                    Action.AUTHORIZE_ENTITY_REPLAY, state.snapshot());
            String authorizationPayload = new String(authorization.payload(), StandardCharsets.UTF_8);
            for (String required : new String[]{
                    "\"capability\":\"entity_replay\"", "\"world_mutation\":false",
                    "\"stop\":\"terminal_cancel\"", "\"rollback\":\"remove_display_echoes\""}) {
                check(authorizationPayload.contains(required), "authorization payload missing " + required);
            }
            commitAsGroup(state, Action.AUTHORIZE_ENTITY_REPLAY, 6);
            check(state.pendingAfter(0).size() == 4, "six authorizations create one authorization receipt");
            check(state.snapshot().stage() == MorrowStage.CURIOUS,
                    "only complete proof plus explicit authorization advances to curious");

            MorrowLocalState restarted = MorrowLocalState.open(journal, RELEASE);
            check(restarted.snapshot().stage() == MorrowStage.CURIOUS,
                    "dialog receipts reconstruct exactly after restart");
            check(restarted.pendingAfter(0).size() == 4, "restart retains exact local receipt count");

            String bukkit = Files.readString(Path.of(
                    "src/main/java/com/observance/watcher/morrow/dialog/BukkitMorrowDialogs.java"),
                    StandardCharsets.UTF_8);
            for (String required : new String[]{
                    "Dialog.create", "canCloseWithEscape(true)", "PlayerCustomClickEvent",
                    "DialogType.confirmation", "DialogType.multiAction", "MorrowLocalState.CommitResult"}) {
                check(bukkit.contains(required), "native dialog adapter missing " + required);
            }
            for (String forbidden : new String[]{"setBlockData(", "setType(", "breakNaturally(", "Async"}) {
                check(!bukkit.contains(forbidden), "item 5 must stop before world mutation: " + forbidden);
            }
            System.out.println("MORROW DIALOG AUTHORITY: PASS players=1/2/6 receipts=4 escape_safe=true");
        } finally {
            Files.deleteIfExists(journal);
            Files.deleteIfExists(directory);
        }
    }

    private static void commitAsGroup(MorrowLocalState state, Action action, int playerCount) throws Exception {
        byte[] firstPayload = null;
        long firstSequence = -1;
        for (int player = 0; player < playerCount; player++) {
            Decision decision = MorrowDialogAuthority.decide(action, state.snapshot());
            check(decision.status() == MorrowDialogAuthority.Status.READY && decision.commitsReceipt(),
                    action + " must be ready and receipt-producing");
            if (firstPayload == null) firstPayload = decision.payload();
            else check(Arrays.equals(firstPayload, decision.payload()), action + " payload is group-stable");
            MorrowLocalState.CommitResult result = state.commit(
                    decision.eventKey(), decision.idempotencyKey(), decision.payload());
            if (player == 0) {
                check(result.created(), action + " first callback creates receipt");
                firstSequence = result.sequence();
            } else {
                check(!result.created() && result.sequence() == firstSequence,
                        action + " repeated player callback is idempotent");
            }
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
