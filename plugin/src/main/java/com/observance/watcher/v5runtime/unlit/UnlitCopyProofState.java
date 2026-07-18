package com.observance.watcher.v5runtime.unlit;

import com.observance.watcher.m2runtime.LocalPrimaryJournal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Local-primary authority for the bounded surface-to-Unlit copy proof.
 *
 * <p>The committed input is six allowlisted civic-work tokens. It contains no free text, player
 * identity, inventory content, arbitrary build data, chat, logs, or inferred personal material.
 * The copy is deterministic: it mirrors the two rows and changes the first copied RECORD token to
 * WATCH. That one authored alteration demonstrates institutional rewriting without claiming what
 * the Dark is.</p>
 */
public final class UnlitCopyProofState {
    public static final int CELL_COUNT = 6;
    public static final String EVENT_TYPE = "unlit_copy_proof_committed";
    private static final String PAYLOAD_VERSION = "unlit-copy-v1";

    public enum Token {
        WATER("water"), HEAT("heat"), WATCH("watch"), RECORD("record");

        private final String wire;
        Token(String wire) { this.wire = wire; }
        public String wire() { return wire; }

        public static Token parse(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            for (Token token : values()) if (token.wire.equals(normalized)) return token;
            throw new IllegalArgumentException("unknown copy token: " + normalized);
        }
    }

    public enum CommitStatus { COMMITTED, IDEMPOTENT, LOCKED }

    public record Snapshot(List<Token> surface, List<Token> unlit, String patternSha256,
                           long journalSequence) {
        public Snapshot {
            surface = List.copyOf(surface);
            unlit = List.copyOf(unlit);
        }
    }

    public record CommitOutcome(CommitStatus status, Snapshot snapshot) { }

    private final LocalPrimaryJournal journal;
    private Snapshot committed;

    private UnlitCopyProofState(LocalPrimaryJournal journal, Snapshot committed) {
        this.journal = journal;
        this.committed = committed;
    }

    public static UnlitCopyProofState open(Path path) throws IOException {
        LocalPrimaryJournal journal = LocalPrimaryJournal.open(path);
        Snapshot committed = null;
        for (LocalPrimaryJournal.Receipt receipt : journal.after(0)) {
            if (!EVENT_TYPE.equals(receipt.eventType())) {
                throw new IOException("unexpected Unlit copy journal event: " + receipt.eventType());
            }
            Snapshot decoded = decode(receipt.payload(), receipt.sequence());
            if (committed != null && !committed.equals(decoded)) {
                throw new IOException("Unlit copy journal contains more than one committed pattern");
            }
            committed = decoded;
        }
        return new UnlitCopyProofState(journal, committed);
    }

    public synchronized Snapshot committed() { return committed; }

    public synchronized CommitOutcome commit(List<Token> proposed) throws IOException {
        List<Token> surface = validate(proposed);
        String patternHash = patternHash(surface);
        if (committed != null) {
            return new CommitOutcome(committed.patternSha256().equals(patternHash)
                    ? CommitStatus.IDEMPOTENT : CommitStatus.LOCKED, committed);
        }
        List<Token> copied = alter(surface);
        byte[] payload = encode(surface, copied, patternHash);
        LocalPrimaryJournal.Receipt receipt = journal.append(
                "unlit-copy:v1:" + patternHash, EVENT_TYPE, payload);
        committed = new Snapshot(surface, copied, patternHash, receipt.sequence());
        return new CommitOutcome(CommitStatus.COMMITTED, committed);
    }

    public static List<Token> validate(List<Token> proposed) {
        Objects.requireNonNull(proposed, "proposed");
        if (proposed.size() != CELL_COUNT || proposed.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("copy proof requires exactly six allowlisted tokens");
        }
        Set<Token> distinct = new LinkedHashSet<>(proposed);
        if (distinct.size() < 3) {
            throw new IllegalArgumentException("copy proof requires at least three civic roles");
        }
        if (!distinct.contains(Token.RECORD)) {
            throw new IllegalArgumentException("copy proof requires one record token");
        }
        return List.copyOf(proposed);
    }

    /** Mirrors each three-cell row, then makes exactly one RECORD-to-WATCH institutional edit. */
    public static List<Token> alter(List<Token> proposed) {
        List<Token> surface = validate(proposed);
        List<Token> copied = new ArrayList<>(List.of(
                surface.get(2), surface.get(1), surface.get(0),
                surface.get(5), surface.get(4), surface.get(3)));
        int record = copied.indexOf(Token.RECORD);
        if (record < 0) throw new IllegalStateException("validated copy has no record token");
        copied.set(record, Token.WATCH);
        return List.copyOf(copied);
    }

    public static String patternHash(List<Token> surface) {
        List<Token> validated = validate(surface);
        return sha256(validated.stream().map(Token::wire).reduce((a, b) -> a + "," + b)
                .orElseThrow().getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] encode(List<Token> surface, List<Token> copied, String hash) {
        return (PAYLOAD_VERSION + "\n" + wire(surface) + "\n" + wire(copied) + "\n" + hash)
                .getBytes(StandardCharsets.UTF_8);
    }

    private static Snapshot decode(byte[] payload, long sequence) throws IOException {
        String[] lines = new String(payload, StandardCharsets.UTF_8).split("\\n", -1);
        if (lines.length != 4 || !PAYLOAD_VERSION.equals(lines[0])) {
            throw new IOException("invalid Unlit copy proof payload");
        }
        try {
            List<Token> surface = validate(parseWire(lines[1]));
            List<Token> copied = parseWire(lines[2]);
            String expectedHash = patternHash(surface);
            if (!expectedHash.equals(lines[3]) || !alter(surface).equals(copied)) {
                throw new IOException("Unlit copy proof payload failed deterministic verification");
            }
            return new Snapshot(surface, copied, expectedHash, sequence);
        } catch (IllegalArgumentException exception) {
            throw new IOException("invalid Unlit copy proof token payload", exception);
        }
    }

    private static List<Token> parseWire(String line) {
        String[] values = line.split(",", -1);
        if (values.length != CELL_COUNT) {
            throw new IllegalArgumentException("copy proof payload requires exactly six tokens");
        }
        List<Token> result = new ArrayList<>(values.length);
        for (String value : values) result.add(Token.parse(value));
        return List.copyOf(result);
    }

    private static String wire(List<Token> tokens) {
        return tokens.stream().map(Token::wire).reduce((a, b) -> a + "," + b).orElseThrow();
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
