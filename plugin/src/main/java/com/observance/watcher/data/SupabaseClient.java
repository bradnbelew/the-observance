package com.observance.watcher.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.observance.watcher.config.ObservanceConfig;
import com.observance.watcher.data.rows.ArcStateRow;
import com.observance.watcher.data.rows.BaseRow;
import com.observance.watcher.data.rows.BeatQueueRow;
import com.observance.watcher.data.rows.CustomComplianceRow;
import com.observance.watcher.data.rows.DossierRow;
import com.observance.watcher.data.rows.EventLogRow;
import com.observance.watcher.data.rows.HeatmapCellRow;
import com.observance.watcher.data.rows.PlayerRow;
import com.observance.watcher.data.rows.SettingsRow;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * ASYNC PostgREST client for the Observance Supabase. Talks to {@code <url>/<table>} with the
 * service-role key as BOTH {@code apikey} and {@code Authorization: Bearer}.
 *
 * <h2>Hard guarantees</h2>
 * <ul>
 *   <li><b>Never throws into callers.</b> Every public method returns a {@link SupabaseResult};
 *       failures are captured, not propagated.</li>
 *   <li><b>Async only.</b> These methods BLOCK on network I/O and therefore MUST be called from
 *       an async thread (use {@code Scheduler.runAsync*} / {@code asyncThenMain}). They never
 *       touch Bukkit objects, so they are safe off the main thread.</li>
 *   <li><b>Graceful degradation.</b> If Supabase is unreachable, writes are queued (bounded) and
 *       flushed on the next successful call; reads return a failed result and callers skip.</li>
 *   <li><b>Secret hygiene.</b> The service key is held privately and never logged.</li>
 * </ul>
 *
 * <p>Threading: this class is thread-safe. The underlying {@link HttpClient} is shared; the
 * offline queue is guarded by its own lock.
 */
public final class SupabaseClient {

    private final ObservanceConfig config;
    private final Logger logger;
    private final Gson gson;
    private final HttpClient http;

    // Bounded offline write queue. Each entry is a self-contained re-runnable write attempt.
    private final Deque<QueuedWrite> offlineQueue = new ArrayDeque<>();
    private final Object queueLock = new Object();

    // Tracks connectivity so callers (and the dashboard health view) can read status cheaply.
    private volatile boolean lastCallSucceeded = false;
    private volatile long lastSuccessMs = 0L;
    private volatile long lastFailureMs = 0L;

    private static final java.lang.reflect.Type LIST_BEAT =
            new TypeToken<List<BeatQueueRow>>() {}.getType();
    private static final java.lang.reflect.Type LIST_SETTINGS =
            new TypeToken<List<SettingsRow>>() {}.getType();
    private static final java.lang.reflect.Type LIST_ARC =
            new TypeToken<List<ArcStateRow>>() {}.getType();

    private record QueuedWrite(String description, Supplier<SupabaseResult<Void>> attempt) { }

    public SupabaseClient(ObservanceConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
        // Drop nulls so partial POJOs upsert only the fields we set.
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.connectTimeoutMs()))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /* ==================================================================== */
    /*  Status                                                              */
    /* ==================================================================== */

    public boolean isConfigured() { return config.isConfigured(); }
    public boolean lastCallSucceeded() { return lastCallSucceeded; }
    public long lastSuccessMs() { return lastSuccessMs; }
    public long lastFailureMs() { return lastFailureMs; }

    public int queuedWriteCount() {
        synchronized (queueLock) { return offlineQueue.size(); }
    }

    /* ==================================================================== */
    /*  WRITE helpers (per table)                                           */
    /* ==================================================================== */

    /** Upsert a player on join (mc_uuid conflict target). */
    public SupabaseResult<Void> upsertPlayer(PlayerRow row) {
        return upsert("players", "mc_uuid", row, "upsertPlayer");
    }

    /** Upsert a dossier (mc_uuid conflict target). */
    public SupabaseResult<Void> upsertDossier(DossierRow row) {
        return upsert("dossiers", "mc_uuid", row, "upsertDossier");
    }

    /** Upsert a custom-compliance row (mc_uuid,custom_key conflict target). */
    public SupabaseResult<Void> upsertCompliance(CustomComplianceRow row) {
        return upsert("custom_compliance", "mc_uuid,custom_key", row, "upsertCompliance");
    }

    /** Upsert a heatmap cell (world,cell_x,cell_z conflict target). */
    public SupabaseResult<Void> upsertHeatmapCell(HeatmapCellRow row) {
        return upsert("heatmap_cells", "world,cell_x,cell_z", row, "upsertHeatmapCell");
    }

    /** Upsert a detected base (id conflict target). */
    public SupabaseResult<Void> upsertBase(BaseRow row) {
        return upsert("bases", "id", row, "upsertBase");
    }

    /**
     * Insert an event_log row. Special-cased to be the LOWEST-risk write: on hard failure it is
     * NOT re-queued (to avoid an error-logging feedback loop filling the queue), it just drops.
     */
    public SupabaseResult<Void> insertEventLog(EventLogRow row) {
        if (!config.isConfigured()) return SupabaseResult.queued();
        String body = gson.toJson(row);
        SupabaseResult<Void> r = doWrite("POST", "event_log", "", body, false, "insertEventLog");
        // Never re-queue event_log; if it failed it failed. Avoids recursive log storms.
        return r;
    }

    /* ==================================================================== */
    /*  READ helpers (per table)                                            */
    /* ==================================================================== */

    /**
     * Read pending+approved beats, optionally limited, ordered by priority desc then created_at.
     * Returns an empty list on any failure (never null, never throws).
     */
    public SupabaseResult<List<BeatQueueRow>> fetchActionableBeats(int limit) {
        if (!config.isConfigured()) {
            return SupabaseResult.ok(0, Collections.emptyList());
        }
        String q = "status=in.(pending,approved)"
                + "&order=priority.desc.nullslast,created_at.asc"
                + "&limit=" + Math.max(1, limit);
        SupabaseResult<List<BeatQueueRow>> r = doRead("beat_queue", q, LIST_BEAT, "fetchActionableBeats");
        if (!r.ok() || r.value() == null) {
            return SupabaseResult.ok(r.httpStatus(), Collections.emptyList());
        }
        return r;
    }

    /**
     * Mark a beat fired (status='fired', decided_at=now). {@code newStatus} lets callers also
     * record "skipped"/"failed". PATCH filtered by id.
     */
    public SupabaseResult<Void> markBeatDecided(String beatId, String newStatus) {
        if (beatId == null || beatId.isBlank()) {
            return SupabaseResult.fail(0, "blank-beat-id");
        }
        String status = (newStatus == null || newStatus.isBlank()) ? "fired" : newStatus;
        String filter = "id=eq." + enc(beatId);
        String body = "{\"status\":\"" + jsonEscape(status) + "\",\"decided_at\":\""
                + nowIso() + "\"}";
        if (!config.isConfigured()) {
            // Queue the decision so a still-pending beat isn't re-fired after a restart.
            enqueue("markBeatDecided", () ->
                    doWrite("PATCH", "beat_queue", filter, body, true, "markBeatDecided"));
            return SupabaseResult.queued();
        }
        SupabaseResult<Void> r =
                doWrite("PATCH", "beat_queue", filter, body, true, "markBeatDecided");
        if (!r.ok()) {
            // CRITICAL replay guard: a beat was already enacted in-world. If we don't durably record
            // the decision, the next poll re-fetches the still-pending row and RE-FIRES it (after a
            // restart the in-process idempotency set is empty). The PATCH is idempotent, so queueing
            // a retry is safe.
            enqueue("markBeatDecided", () ->
                    doWrite("PATCH", "beat_queue", filter, body, true, "markBeatDecided"));
        }
        return r;
    }

    /** Read a single setting by key. Returns ok with null value if absent. */
    public SupabaseResult<SettingsRow> fetchSetting(String key) {
        if (!config.isConfigured() || key == null) {
            return SupabaseResult.ok(0, null);
        }
        String q = "key=eq." + enc(key) + "&limit=1";
        SupabaseResult<List<SettingsRow>> r = doRead("settings", q, LIST_SETTINGS, "fetchSetting");
        if (!r.ok()) return SupabaseResult.fail(r.httpStatus(), r.error());
        List<SettingsRow> list = r.value();
        SettingsRow first = (list == null || list.isEmpty()) ? null : list.get(0);
        return SupabaseResult.ok(r.httpStatus(), first);
    }

    /**
     * Convenience: read the {@code watcher_sleep} setting. Returns true only on a confirmed true;
     * on ANY failure returns false (fail-open: a DB hiccup never silently mutes the whole game —
     * the local config kill switch is the operator's reliable mute).
     */
    public boolean isWatcherSleeping() {
        SupabaseResult<SettingsRow> r = fetchSetting("watcher_sleep");
        if (!r.ok() || r.value() == null) return false;
        return r.value().asBoolean();
    }

    /** Read current arc state (act). Returns ok with null if absent. */
    public SupabaseResult<ArcStateRow> fetchArcState() {
        if (!config.isConfigured()) return SupabaseResult.ok(0, null);
        String q = "order=updated_at.desc&limit=1";
        SupabaseResult<List<ArcStateRow>> r = doRead("arc_state", q, LIST_ARC, "fetchArcState");
        if (!r.ok()) return SupabaseResult.fail(r.httpStatus(), r.error());
        List<ArcStateRow> list = r.value();
        ArcStateRow first = (list == null || list.isEmpty()) ? null : list.get(0);
        return SupabaseResult.ok(r.httpStatus(), first);
    }

    /* ==================================================================== */
    /*  Offline queue                                                       */
    /* ==================================================================== */

    /**
     * Flush queued writes (call periodically from an async task). Stops early on first failure so
     * it doesn't hammer a still-down endpoint. Returns the number successfully flushed.
     */
    public int flushOfflineQueue() {
        int flushed = 0;
        while (true) {
            QueuedWrite next;
            synchronized (queueLock) {
                next = offlineQueue.peekFirst();
            }
            if (next == null) break;
            SupabaseResult<Void> r = next.attempt().get();
            if (r.ok()) {
                synchronized (queueLock) {
                    offlineQueue.pollFirst();
                }
                flushed++;
            } else {
                break; // still down — leave the rest queued, try again next cycle
            }
        }
        return flushed;
    }

    private void enqueue(String description, Supplier<SupabaseResult<Void>> attempt) {
        synchronized (queueLock) {
            while (offlineQueue.size() >= config.offlineQueueMax() && !offlineQueue.isEmpty()) {
                offlineQueue.pollFirst(); // drop oldest to bound memory
            }
            if (config.offlineQueueMax() > 0) {
                offlineQueue.addLast(new QueuedWrite(description, attempt));
            }
        }
    }

    /* ==================================================================== */
    /*  Generic upsert / read / write core                                  */
    /* ==================================================================== */

    private SupabaseResult<Void> upsert(String table, String onConflict, Object pojo, String ctx) {
        if (pojo == null) return SupabaseResult.fail(0, "null-row");
        if (!config.isConfigured()) {
            // Queue it for when config/connectivity returns.
            String body = gson.toJson(pojo);
            enqueue(ctx, () -> doUpsert(table, onConflict, body, ctx));
            return SupabaseResult.queued();
        }
        String body = gson.toJson(pojo);
        SupabaseResult<Void> r = doUpsert(table, onConflict, body, ctx);
        if (!r.ok()) {
            enqueue(ctx, () -> doUpsert(table, onConflict, body, ctx));
        }
        return r;
    }

    private SupabaseResult<Void> doUpsert(String table, String onConflict, String body, String ctx) {
        String query = "on_conflict=" + enc(onConflict);
        // Prefer: merge-duplicates makes POST act as upsert; resolution=merge-duplicates.
        return doWrite("POST", table, query, body, true, ctx);
    }

    private <T> SupabaseResult<List<T>> doRead(String table, String query,
                                               java.lang.reflect.Type listType, String ctx) {
        return withRetries(ctx, () -> {
            HttpRequest req = baseRequest(table, query)
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();
            if (code >= 200 && code < 300) {
                List<T> parsed;
                try {
                    parsed = gson.fromJson(resp.body(), listType);
                } catch (JsonSyntaxException jse) {
                    markFailure();
                    return SupabaseResult.<List<T>>fail(code, "parse-error");
                }
                markSuccess();
                return SupabaseResult.ok(code, parsed == null ? Collections.<T>emptyList() : parsed);
            }
            markFailure();
            return SupabaseResult.<List<T>>fail(code, "http-" + code);
        }, SupabaseResult.fail(0, "exhausted"));
    }

    /**
     * @param preferReturnMinimal when true, sends Prefer: return=minimal (+ merge-duplicates for upsert)
     */
    private SupabaseResult<Void> doWrite(String method, String table, String query, String body,
                                         boolean preferReturnMinimal, String ctx) {
        return withRetries(ctx, () -> {
            HttpRequest.Builder b = baseRequest(table, query)
                    .header("Content-Type", "application/json");
            if (preferReturnMinimal) {
                // merge-duplicates → upsert behavior; return=minimal → no body back.
                b.header("Prefer", "resolution=merge-duplicates,return=minimal");
            } else {
                b.header("Prefer", "return=minimal");
            }
            HttpRequest req = b.method(method,
                    HttpRequest.BodyPublishers.ofString(body == null ? "" : body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();
            if (code >= 200 && code < 300) {
                markSuccess();
                return SupabaseResult.<Void>ok(code, null);
            }
            markFailure();
            return SupabaseResult.<Void>fail(code, "http-" + code);
        }, SupabaseResult.fail(0, "exhausted"));
    }

    /* ==================================================================== */
    /*  Plumbing                                                            */
    /* ==================================================================== */

    private HttpRequest.Builder baseRequest(String table, String query) {
        String url = config.supabaseUrl() + "/" + table + (query == null || query.isBlank() ? "" : ("?" + query));
        String key = config.serviceKey();
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(config.readTimeoutMs()))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Accept", "application/json");
    }

    /**
     * Run an attempt with bounded retries + jittered exponential backoff. ANY exception is caught
     * and treated as a retryable failure; on exhaustion the supplied fallback is returned. This is
     * the choke point that guarantees we never throw into callers.
     */
    private <R> R withRetries(String ctx, NetCall<R> call, R fallback) {
        int attempts = config.maxRetries() + 1;
        for (int i = 0; i < attempts; i++) {
            try {
                return call.run();
            } catch (Throwable t) {
                markFailure();
                if (i < attempts - 1) {
                    sleepBackoff(i);
                } else {
                    // Last attempt failed with an exception.
                    return fallback;
                }
            }
        }
        return fallback;
    }

    @FunctionalInterface
    private interface NetCall<R> {
        R run() throws Exception;
    }

    private void sleepBackoff(int attemptIndex) {
        long base = config.retryBackoffMs();
        if (base <= 0) return;
        long backoff = base * (1L << Math.min(attemptIndex, 5)); // double each retry, capped
        long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1, base));
        try {
            Thread.sleep(backoff + jitter);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void markSuccess() {
        lastCallSucceeded = true;
        lastSuccessMs = System.currentTimeMillis();
    }

    private void markFailure() {
        lastCallSucceeded = false;
        lastFailureMs = System.currentTimeMillis();
    }

    private static String nowIso() {
        return Instant.now().toString();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    /** ISO-8601 UTC now, exposed for callers building rows. */
    public static String timestampNow() {
        return Instant.now().toString();
    }
}
