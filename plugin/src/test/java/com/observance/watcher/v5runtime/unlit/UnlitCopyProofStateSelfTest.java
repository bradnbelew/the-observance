package com.observance.watcher.v5runtime.unlit;

import com.observance.watcher.v5runtime.unlit.UnlitCopyProofState.CommitStatus;
import com.observance.watcher.v5runtime.unlit.UnlitCopyProofState.Token;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Dependency-free contract test for the privacy-bounded, restart-safe copy proof. */
public final class UnlitCopyProofStateSelfTest {
    private UnlitCopyProofStateSelfTest() { }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("observance-unlit-copy-");
        Path journal = dir.resolve("copy.journal");
        List<Token> surface = List.of(
                Token.WATER, Token.HEAT, Token.RECORD,
                Token.WATCH, Token.WATER, Token.HEAT);

        UnlitCopyProofState state = UnlitCopyProofState.open(journal);
        var committed = state.commit(surface);
        require(committed.status() == CommitStatus.COMMITTED, "first commit must commit");
        require(committed.snapshot().unlit().equals(List.of(
                Token.WATCH, Token.HEAT, Token.WATER,
                Token.HEAT, Token.WATER, Token.WATCH)), "copy alteration drifted");
        require(state.commit(surface).status() == CommitStatus.IDEMPOTENT,
                "same input must be idempotent");
        require(state.commit(List.of(
                Token.RECORD, Token.WATER, Token.HEAT,
                Token.WATCH, Token.HEAT, Token.WATER)).status() == CommitStatus.LOCKED,
                "a different second pattern must be locked");

        UnlitCopyProofState reopened = UnlitCopyProofState.open(journal);
        require(reopened.committed() != null, "restart lost committed proof");
        require(reopened.committed().equals(committed.snapshot()), "restart changed proof bytes");
        String bytes = Files.readString(journal, StandardCharsets.UTF_8);
        require(!bytes.contains("player") && !bytes.contains("name") && !bytes.contains("fear"),
                "journal contains forbidden personalized fields");

        rejects(List.of(Token.WATER, Token.WATER, Token.WATER,
                Token.RECORD, Token.RECORD, Token.RECORD));
        rejects(List.of(Token.WATER, Token.HEAT, Token.WATCH,
                Token.WATER, Token.HEAT, Token.WATCH));
        System.out.println("UnlitCopyProofStateSelfTest PASS");
    }

    private static void rejects(List<Token> tokens) {
        try {
            UnlitCopyProofState.validate(tokens);
            throw new AssertionError("invalid pattern was accepted: " + tokens);
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
