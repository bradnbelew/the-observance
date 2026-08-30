package com.observance.watcher.morrow;

import com.observance.watcher.m2runtime.LocalPrimaryJournal;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ordered asynchronous projection of local-primary Morrow receipts. This class is deliberately
 * Bukkit-free: the worker owns only immutable settings, journal bytes, an HTTPS client, and a cursor.
 */
public final class MorrowEventProjector implements AutoCloseable {
    private static final String CURSOR_SCHEMA = "morrow-projector-cursor-v1";
    private static final String GENESIS = "0".repeat(64);
    private static final Duration IDLE_POLL = Duration.ofMillis(250);

    public enum State { NEW, RUNNING, IDLE, RETRYING, HALTED, CLOSED }
    public enum HaltReason { NONE, COLLISION, WRONG_RELEASE, AUTHENTICATION, REMOTE_REJECTION, INVALID_RESPONSE }

    public record Snapshot(
            State state,
            HaltReason haltReason,
            long projectedSequence,
            int retryAttempt,
            String lastError) {
        public Snapshot {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(haltReason, "haltReason");
            lastError = lastError == null ? "" : lastError;
        }
    }

    record ProjectionRequest(
            long sequence,
            String eventHash,
            String timestamp,
            String signature,
            String body) { }

    record ProjectionResponse(int statusCode, String body) {
        ProjectionResponse {
            body = body == null ? "" : body;
        }
    }

    @FunctionalInterface
    interface Transport {
        ProjectionResponse send(ProjectionRequest request) throws IOException, InterruptedException;
    }

    @FunctionalInterface
    interface Delay {
        void pause(Duration duration) throws InterruptedException;
    }

    private enum StepResult { ADVANCED, IDLE, RETRY, HALTED }

    private record Cursor(String releaseId, long sequence, String eventHash) { }

    private final MorrowLocalState localState;
    private final MorrowRuntimeSettings settings;
    private final CursorStore cursorStore;
    private final Transport transport;
    private final Clock clock;
    private final Delay delay;
    private final Logger logger;
    private final ExecutorService executor;
    private final AtomicBoolean closeRequested = new AtomicBoolean(false);

    private volatile Cursor cursor;
    private volatile State state = State.NEW;
    private volatile HaltReason haltReason = HaltReason.NONE;
    private volatile int retryAttempt;
    private volatile String lastError = "";
    private long inFlightSequence;
    private Instant inFlightOccurredAt;

    private MorrowEventProjector(
            MorrowLocalState localState,
            MorrowRuntimeSettings settings,
            CursorStore cursorStore,
            Transport transport,
            Clock clock,
            Delay delay,
            Logger logger) throws IOException {
        this.localState = Objects.requireNonNull(localState, "localState");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.cursorStore = Objects.requireNonNull(cursorStore, "cursorStore");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.delay = Objects.requireNonNull(delay, "delay");
        this.logger = Objects.requireNonNull(logger, "logger");
        if (!settings.releaseId().equals(localState.releaseId())) {
            throw new IOException("Morrow projector/local journal release mismatch");
        }
        this.cursor = cursorStore.load(settings.releaseId(), localState);
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "observance-morrow-projector");
            thread.setDaemon(true);
            return thread;
        });
    }

    static MorrowEventProjector open(
            MorrowLocalState state,
            MorrowRuntimeSettings settings,
            Path cursorPath,
            Logger logger) throws IOException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(settings.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        Transport transport = request -> {
            HttpRequest httpRequest = HttpRequest.newBuilder(settings.ingestUri())
                    .timeout(settings.requestTimeout())
                    .header("content-type", "application/json; charset=utf-8")
                    .header("x-morrow-timestamp", request.timestamp())
                    .header("x-morrow-signature", request.signature())
                    .header("user-agent", "Observance-Morrow/1")
                    .POST(HttpRequest.BodyPublishers.ofString(request.body(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new ProjectionResponse(response.statusCode(), response.body());
        };
        return new MorrowEventProjector(
                state,
                settings,
                new CursorStore(cursorPath),
                transport,
                Clock.systemUTC(),
                duration -> Thread.sleep(duration.toMillis()),
                logger);
    }

    static MorrowEventProjector forTest(
            MorrowLocalState state,
            MorrowRuntimeSettings settings,
            Path cursorPath,
            Transport transport,
            Clock clock,
            Delay delay) throws IOException {
        return new MorrowEventProjector(
                state,
                settings,
                new CursorStore(cursorPath),
                transport,
                clock,
                delay,
                Logger.getLogger("morrow-projector-selftest"));
    }

    public synchronized void start() {
        if (state != State.NEW) throw new IllegalStateException("Morrow projector already started");
        state = State.RUNNING;
        executor.execute(this::runLoop);
    }

    public Snapshot snapshot() {
        Cursor current = cursor;
        return new Snapshot(state, haltReason, current.sequence(), retryAttempt, lastError);
    }

    /** Deterministic package-private driver used by main()-based Gradle self-tests. */
    void driveForTest(int maximumSteps) throws IOException, InterruptedException {
        if (maximumSteps < 1) throw new IllegalArgumentException("maximumSteps must be positive");
        if (state == State.NEW) state = State.RUNNING;
        for (int step = 0; step < maximumSteps && !closeRequested.get(); step++) {
            StepResult result = step();
            if (result == StepResult.IDLE || result == StepResult.HALTED) return;
        }
    }

    private void runLoop() {
        try {
            while (!closeRequested.get() && haltReason == HaltReason.NONE) {
                StepResult result = step();
                if (result == StepResult.IDLE) delay.pause(IDLE_POLL);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Throwable failure) {
            halt(HaltReason.INVALID_RESPONSE,
                    "projector worker failed: " + failure.getClass().getSimpleName());
            logger.log(Level.SEVERE, "Morrow projector failed closed", failure);
        } finally {
            if (haltReason == HaltReason.NONE) state = State.CLOSED;
        }
    }

    private StepResult step() throws IOException, InterruptedException {
        if (haltReason != HaltReason.NONE) return StepResult.HALTED;
        List<LocalPrimaryJournal.Receipt> pending = localState.pendingAfter(cursor.sequence());
        if (pending.isEmpty()) {
            state = State.IDLE;
            retryAttempt = 0;
            inFlightSequence = 0;
            inFlightOccurredAt = null;
            return StepResult.IDLE;
        }

        LocalPrimaryJournal.Receipt receipt = pending.get(0); // strict order; never skip a failure
        if (inFlightSequence != receipt.sequence()) {
            inFlightSequence = receipt.sequence();
            inFlightOccurredAt = clock.instant();
            retryAttempt = 0;
        }
        state = State.RUNNING;
        ProjectionRequest request = request(receipt, inFlightOccurredAt, clock.instant());
        ProjectionResponse response;
        try {
            response = transport.send(request);
        } catch (IOException failure) {
            return retry("transport outage: " + failure.getClass().getSimpleName());
        }

        String responseRelease = jsonString(response.body(), "releaseId");
        if (responseRelease != null && !settings.releaseId().equals(responseRelease)) {
            return halt(HaltReason.WRONG_RELEASE, "remote response release mismatch");
        }
        String remoteStatus = jsonString(response.body(), "status");

        if (response.statusCode() == 200
                && ("committed".equals(remoteStatus) || "duplicate".equals(remoteStatus))) {
            if (responseRelease == null) return halt(HaltReason.INVALID_RESPONSE, "success omitted release binding");
            Cursor advanced = new Cursor(settings.releaseId(), receipt.sequence(), receipt.eventHash());
            cursorStore.persist(advanced);
            cursor = advanced;
            retryAttempt = 0;
            lastError = "";
            inFlightSequence = 0;
            inFlightOccurredAt = null;
            return StepResult.ADVANCED;
        }
        if (response.statusCode() == 409 && "collision".equals(remoteStatus)) {
            if (responseRelease == null) return halt(HaltReason.INVALID_RESPONSE, "collision omitted release binding");
            return halt(HaltReason.COLLISION, "remote idempotency collision");
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            return halt(HaltReason.AUTHENTICATION, "remote authentication rejected projector");
        }
        if (response.statusCode() == 422 && "blocked".equals(remoteStatus)) {
            if (responseRelease == null) return halt(HaltReason.INVALID_RESPONSE, "blocked response omitted release binding");
            return retry("remote prerequisite or campaign binding is not ready");
        }
        if (response.statusCode() == 408 || response.statusCode() == 425 || response.statusCode() == 429
                || response.statusCode() >= 500) {
            return retry("remote unavailable (HTTP " + response.statusCode() + ")");
        }
        if (response.statusCode() >= 400 && response.statusCode() < 500) {
            return halt(HaltReason.REMOTE_REJECTION, "remote rejected event (HTTP " + response.statusCode() + ")");
        }
        return halt(HaltReason.INVALID_RESPONSE, "unexpected projector response (HTTP "
                + response.statusCode() + ")");
    }

    private StepResult retry(String error) throws InterruptedException {
        retryAttempt++;
        lastError = error;
        state = State.RETRYING;
        long initial = settings.initialRetry().toMillis();
        long maximum = settings.maximumRetry().toMillis();
        int shift = Math.min(retryAttempt - 1, 30);
        long delayMillis = initial > (Long.MAX_VALUE >> shift) ? maximum : initial << shift;
        delay.pause(Duration.ofMillis(Math.min(maximum, delayMillis)));
        return StepResult.RETRY;
    }

    private StepResult halt(HaltReason reason, String error) {
        haltReason = reason;
        lastError = error;
        state = State.HALTED;
        logger.severe("Morrow projector halted: " + reason + " (" + error + ")");
        return StepResult.HALTED;
    }

    private ProjectionRequest request(
            LocalPrimaryJournal.Receipt receipt,
            Instant occurredAt,
            Instant requestTime) throws IOException {
        String payload = utf8JsonObject(receipt.payload());
        String body = "{"
                + "\"campaignId\":\"" + escape(settings.campaignId().toString()) + "\","
                + "\"releaseId\":\"" + escape(settings.releaseId()) + "\","
                + "\"eventKey\":\"" + escape(receipt.eventType()) + "\","
                + "\"idempotencyKey\":\"" + escape(receipt.idempotencyKey()) + "\","
                + "\"actorMinecraftUuid\":null,"
                + "\"occurredAt\":\"" + DateTimeFormatter.ISO_INSTANT.format(occurredAt) + "\","
                + "\"payload\":" + payload
                + "}";
        String timestamp = Long.toString(requestTime.getEpochSecond());
        String signature = "v1=" + hmac(settings.ingestSecret(), timestamp + "." + body);
        return new ProjectionRequest(receipt.sequence(), receipt.eventHash(), timestamp, signature, body);
    }

    static String hmac(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("HmacSHA256 is unavailable", failure);
        }
    }

    private static String utf8JsonObject(byte[] bytes) throws IOException {
        String decoded;
        try {
            decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString()
                    .trim();
        } catch (CharacterCodingException failure) {
            throw new IOException("Morrow payload is not valid UTF-8", failure);
        }
        if (decoded.length() < 2 || decoded.charAt(0) != '{' || decoded.charAt(decoded.length() - 1) != '}') {
            throw new IOException("Morrow payload must be a JSON object");
        }
        return decoded;
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (current < 0x20) out.append(String.format("\\u%04x", (int) current));
                    else out.append(current);
                }
            }
        }
        return out.toString();
    }

    private static String jsonString(String body, String field) {
        String marker = "\"" + field + "\"";
        int key = body.indexOf(marker);
        if (key < 0) return null;
        int colon = body.indexOf(':', key + marker.length());
        if (colon < 0) return null;
        int quote = colon + 1;
        while (quote < body.length() && Character.isWhitespace(body.charAt(quote))) quote++;
        if (quote >= body.length() || body.charAt(quote) != '"') return null;
        int end = body.indexOf('"', quote + 1);
        if (end < 0 || body.substring(quote + 1, end).indexOf('\\') >= 0) return null;
        return body.substring(quote + 1, end);
    }

    @Override
    public void close() {
        if (!closeRequested.compareAndSet(false, true)) return;
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warning("Morrow projector did not stop within five seconds");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        if (haltReason == HaltReason.NONE) state = State.CLOSED;
    }

    private static final class CursorStore {
        private final Path path;

        private CursorStore(Path path) {
            this.path = Objects.requireNonNull(path, "path");
        }

        private Cursor load(String releaseId, MorrowLocalState localState) throws IOException {
            if (!Files.exists(path)) return new Cursor(releaseId, 0, GENESIS);
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            if (lines.size() != 4 || !CURSOR_SCHEMA.equals(lines.get(0))) {
                throw new IOException("invalid Morrow projector cursor schema");
            }
            String storedRelease = value(lines.get(1), "release=");
            String storedSequence = value(lines.get(2), "sequence=");
            String storedHash = value(lines.get(3), "event-hash=");
            if (!releaseId.equals(storedRelease)) {
                throw new IOException("Morrow projector cursor release mismatch");
            }
            long sequence;
            try {
                sequence = Long.parseLong(storedSequence);
            } catch (NumberFormatException failure) {
                throw new IOException("invalid Morrow projector cursor sequence", failure);
            }
            if (sequence <= 0 || !storedHash.matches("[0-9a-f]{64}")) {
                throw new IOException("invalid Morrow projector cursor value");
            }
            LocalPrimaryJournal.Receipt receipt = localState.projectedReceipt(sequence);
            if (receipt == null || !storedHash.equals(receipt.eventHash())) {
                throw new IOException("Morrow projector cursor does not bind to the local journal");
            }
            return new Cursor(releaseId, sequence, storedHash);
        }

        private void persist(Cursor cursor) throws IOException {
            Path absolute = path.toAbsolutePath();
            Path parent = absolute.getParent();
            if (parent == null) throw new IOException("Morrow projector cursor has no parent");
            Files.createDirectories(parent);
            Path temporary = Files.createTempFile(parent, path.getFileName().toString(), ".tmp");
            try {
                String content = CURSOR_SCHEMA + "\n"
                        + "release=" + cursor.releaseId() + "\n"
                        + "sequence=" + cursor.sequence() + "\n"
                        + "event-hash=" + cursor.eventHash() + "\n";
                try (FileChannel channel = FileChannel.open(
                        temporary,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    channel.write(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
                    channel.force(true);
                }
                try {
                    Files.move(temporary, absolute,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException failure) {
                    throw new IOException("filesystem does not support atomic Morrow cursor replacement", failure);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        }

        private static String value(String line, String prefix) throws IOException {
            if (!line.startsWith(prefix) || line.length() == prefix.length()) {
                throw new IOException("invalid Morrow projector cursor field");
            }
            return line.substring(prefix.length());
        }
    }
}
