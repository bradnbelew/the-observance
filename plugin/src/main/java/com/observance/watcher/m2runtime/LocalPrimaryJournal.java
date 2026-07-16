package com.observance.watcher.m2runtime;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Local-primary, restart-safe append journal. Remote surfaces only project these receipts. */
public final class LocalPrimaryJournal {
    public record Receipt(long sequence, String previousHash, String eventHash, String idempotencyKey,
                          String eventType, byte[] payload) {
        public Receipt {
            payload = payload.clone();
        }
        @Override public byte[] payload() { return payload.clone(); }
    }

    private static final String GENESIS = "0".repeat(64);
    private final Path path;
    private final List<Receipt> receipts;
    private final Map<String, Receipt> byIdempotency;

    private LocalPrimaryJournal(Path path, List<Receipt> receipts) {
        this.path = path;
        this.receipts = new ArrayList<>(receipts);
        this.byIdempotency = new LinkedHashMap<>();
        for (Receipt receipt : receipts) {
            if (byIdempotency.put(receipt.idempotencyKey(), receipt) != null) {
                throw new IllegalStateException("duplicate idempotency key in local journal");
            }
        }
    }

    public static LocalPrimaryJournal open(Path path) throws IOException {
        if (!Files.exists(path)) return new LocalPrimaryJournal(path, List.of());
        List<Receipt> loaded = new ArrayList<>();
        String previous = GENESIS;
        long expectedSequence = 1;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            String[] fields = line.split("\\t", -1);
            if (fields.length != 6) throw new IOException("invalid local journal field count");
            long sequence;
            try { sequence = Long.parseLong(fields[0]); }
            catch (NumberFormatException exception) { throw new IOException("invalid local sequence", exception); }
            byte[] payload;
            try { payload = Base64.getUrlDecoder().decode(fields[5]); }
            catch (IllegalArgumentException exception) { throw new IOException("invalid local payload", exception); }
            String computed = eventHash(sequence, fields[1], fields[3], fields[4], payload);
            if (sequence != expectedSequence || !previous.equals(fields[1]) || !computed.equals(fields[2])) {
                throw new IOException("local journal hash-chain verification failed at sequence " + sequence);
            }
            loaded.add(new Receipt(sequence, fields[1], fields[2], fields[3], fields[4], payload));
            previous = fields[2];
            expectedSequence++;
        }
        return new LocalPrimaryJournal(path, loaded);
    }

    public synchronized Receipt append(String idempotencyKey, String eventType, byte[] payload) throws IOException {
        if (idempotencyKey == null || idempotencyKey.isBlank() || eventType == null || eventType.isBlank()
                || idempotencyKey.indexOf('\t') >= 0 || idempotencyKey.indexOf('\n') >= 0
                || idempotencyKey.indexOf('\r') >= 0 || eventType.indexOf('\t') >= 0
                || eventType.indexOf('\n') >= 0 || eventType.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("idempotency key and event type are required");
        }
        Objects.requireNonNull(payload, "payload");
        Receipt prior = byIdempotency.get(idempotencyKey);
        if (prior != null) {
            if (!prior.eventType().equals(eventType) || !MessageDigest.isEqual(prior.payload(), payload)) {
                throw new IllegalStateException("idempotency key reused with different event bytes");
            }
            return prior;
        }
        long sequence = receipts.size() + 1L;
        String previous = receipts.isEmpty() ? GENESIS : receipts.get(receipts.size() - 1).eventHash();
        Receipt receipt = new Receipt(sequence, previous,
                eventHash(sequence, previous, idempotencyKey, eventType, payload), idempotencyKey, eventType, payload);
        List<Receipt> next = new ArrayList<>(receipts);
        next.add(receipt);
        persist(next);
        receipts.add(receipt);
        byIdempotency.put(idempotencyKey, receipt);
        return receipt;
    }

    public synchronized List<Receipt> after(long sequence) {
        return receipts.stream().filter(receipt -> receipt.sequence() > sequence).toList();
    }

    private void persist(List<Receipt> next) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent == null) throw new IOException("journal path has no parent");
        Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, path.getFileName().toString(), ".tmp");
        try {
            StringBuilder bytes = new StringBuilder();
            for (Receipt receipt : next) bytes.append(encode(receipt)).append('\n');
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                channel.write(ByteBuffer.wrap(bytes.toString().getBytes(StandardCharsets.UTF_8)));
                channel.force(true);
            }
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("filesystem does not support atomic local-primary replacement", exception);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static String encode(Receipt receipt) {
        return receipt.sequence() + "\t" + receipt.previousHash() + "\t" + receipt.eventHash() + "\t"
                + receipt.idempotencyKey() + "\t" + receipt.eventType() + "\t"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(receipt.payload());
    }

    private static String eventHash(long sequence, String previous, String key, String type, byte[] payload) {
        String payloadHash = sha256(payload);
        return sha256((sequence + "\n" + previous + "\n" + key + "\n" + type + "\n" + payloadHash)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
