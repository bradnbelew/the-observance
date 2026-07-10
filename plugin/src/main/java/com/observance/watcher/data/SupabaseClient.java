package com.observance.watcher.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.observance.watcher.config.ObservanceConfig;
import com.observance.watcher.data.rows.ArcStateRow;
import com.observance.watcher.data.rows.AnswerAttemptReadRow;
import com.observance.watcher.data.rows.BaseRow;
import com.observance.watcher.data.rows.BeatQueueRow;
import com.observance.watcher.data.rows.CustomComplianceRow;
import com.observance.watcher.data.rows.DossierRow;
import com.observance.watcher.data.rows.EventLogRow;
import com.observance.watcher.data.rows.HeatmapCellRow;
import com.observance.watcher.data.rows.HintRow;
import com.observance.watcher.data.rows.NpcQuestRow;
import com.observance.watcher.data.rows.ObservationRow;
import com.observance.watcher.data.rows.PlayerLookupRow;
import com.observance.watcher.data.rows.PlayerRow;
import com.observance.watcher.data.rows.PuzzleRow;
import com.observance.watcher.data.rows.SettingsRow;
import com.observance.watcher.data.rows.SolveReadRow;

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
import java.util.logging.Level;
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
    private static final java.lang.reflect.Type LIST_PUZZLE =
            new TypeToken<List<PuzzleRow>>() {}.getType();
    private static final java.lang.reflect.Type LIST_SOLVE_READ =
            new TypeToken<List<SolveReadRow>>() {}.getType();
    private static final java.lang.reflect.Type LIST_ATTEMPT_READ =
            new TypeToken<List<AnswerAttemptReadRow>>() {}.getType();
    private static final java.lang.reflect.Type LIST_HINT =
            new TypeToken<List<HintRow>>() {}.getType();
    private static final java.lang.reflect.Type LIST_PLAYER_LOOKUP =
            new TypeToken<List<PlayerLookupRow>>() {}.getType();
    private static final java.lang.reflect.Type LIST_NPC_QUEST =
            new TypeToken<List<NpcQuestRow>>() {}.getType();

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

    /**
     * Upsert a townsfolk quest row — the offer → active → done errand state (Wave S-G).
     * Conflict target is {@code (player_id, quest_key)} (the table's unique index), so re-writing the
     * same quest at a new status merges rather than duplicates. {@code player_id} must be the resolved
     * {@code players.id} (see {@link #fetchPlayerByUuid}), never the raw mc_uuid. Queues on failure.
     */
    public SupabaseResult<Void> upsertQuest(NpcQuestRow row) {
        return upsert("npc_quests", "player_id,quest_key", row, "upsertQuest");
    }

    /**
     * Upsert a detected base. Conflict target is {@code owner_uuid} (a text column with a unique index,
     * guaranteed by the concurrent SQL migration) — NOT the bigint {@code id} PK, which a UUID string
     * cannot populate (the historical UUID-into-bigint upsert 400'd). The row never sends {@code id}
     * (left null → omitted by Gson), so the DB assigns the serial PK.
     */
    public SupabaseResult<Void> upsertBase(BaseRow row) {
        return upsert("bases", "owner_uuid", row, "upsertBase");
    }

    /**
     * INSERT an {@code observations} row — the Observer Tier-1 chat capture (0009_observations.sql).
     * Not an upsert: every utterance is a NEW row. Mirrors {@link #insertAnswerAttempt}'s fire-and-forget
     * durability — on failure it is queued (bounded) like other writes. Never throws.
     *
     * <p>Consent is enforced by the CALLER (the global {@code observer_capture} switch) before we ever get
     * here; this method only writes. The per-player {@code observer_opt_out} gate is enforced before
     * insertion by callers via {@link #observerOptedOut(String)}; the showrunner repeats it before quoting.
     */
    public SupabaseResult<Void> insertObservation(ObservationRow row) {
        if (row == null) return SupabaseResult.fail(0, "null-row");
        String body = gson.toJson(row);
        if (!config.isConfigured()) {
            enqueue("insertObservation", () ->
                    doWrite("POST", "observations", "", body, false, "insertObservation"));
            return SupabaseResult.queued();
        }
        SupabaseResult<Void> r = doWrite("POST", "observations", "", body, false, "insertObservation");
        if (!r.ok()) {
            enqueue("insertObservation", () ->
                    doWrite("POST", "observations", "", body, false, "insertObservation"));
        }
        return r;
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
     * Read <b>approved</b> beats only, optionally limited, ordered by priority desc then created_at.
     * Returns an empty list on any failure (never null, never throws).
     *
     * <p><b>Approval gate (load-bearing).</b> The poller fires {@code status='approved'} ONLY.
     * {@code 'pending'} beats — everything the showrunner/dashboard queues in CONFIRM mode, plus the
     * Accepting trigger — sit untouched until a human flips them to {@code 'approved'} in the
     * dashboard. This is what makes the dashboard's approve/skip buttons real rather than cosmetic.
     * Player-EARNED beats (oracle unlocks, whisper tolls) are inserted as {@code 'approved'} by their
     * producers ({@link #insertBeat} / the bot) so they fire immediately and never wait on a human.
     */
    public SupabaseResult<List<BeatQueueRow>> fetchActionableBeats(int limit) {
        if (!config.isConfigured()) {
            return SupabaseResult.ok(0, Collections.emptyList());
        }
        String q = "status=eq.approved"
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

    /**
     * Atomically claim an approved beat before world mutation. Returns true only when this JVM won the
     * {@code approved -> firing} transition; false means another process claimed it, the row was no
     * longer approved, or Supabase could not durably record the claim. In every false case the caller must
     * skip enactment rather than risk a cross-restart or two-instance double-fire.
     */
    public boolean claimBeatForFiring(String beatId) {
        if (beatId == null || beatId.isBlank() || !config.isConfigured()) {
            return false;
        }
        String filter = "id=eq." + enc(beatId) + "&status=eq.approved";
        String body = "{\"status\":\"firing\"}";
        return withRetries("claimBeatForFiring", () -> {
            HttpRequest req = baseRequest("beat_queue", filter)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();
            if (code >= 200 && code < 300) {
                markSuccess();
                String b = resp.body();
                return b != null && b.trim().length() > 2; // "[]" means no approved row matched.
            }
            markFailure();
            logFailure("claimBeatForFiring", "http-" + code);
            return false;
        }, false);
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
        // arc_state is single-row (id=1 check constraint) — read it directly by PK rather than
        // ordering the whole table, so a stray second row could never surface the wrong state.
        String q = "id=eq.1&limit=1";
        SupabaseResult<List<ArcStateRow>> r = doRead("arc_state", q, LIST_ARC, "fetchArcState");
        if (!r.ok()) return SupabaseResult.fail(r.httpStatus(), r.error());
        List<ArcStateRow> list = r.value();
        ArcStateRow first = (list == null || list.isEmpty()) ? null : list.get(0);
        return SupabaseResult.ok(r.httpStatus(), first);
    }

    /**
     * Read a player's townsfolk quest rows (offer/active/done state) by resolved {@code players.id}.
     * Used to LAZILY LOAD a player's quest states into the listener's in-memory map on first
     * interaction, so the payoff line survives a restart. Mirrors {@link #fetchActionableBeats}: same
     * async/graceful contract — returns an empty list on ANY failure (never null, never throws), and
     * an unloaded player is simply treated as not-offered (a re-offer is harmless + idempotent).
     */
    public SupabaseResult<List<NpcQuestRow>> fetchQuestsForPlayer(String playerId) {
        if (!config.isConfigured() || playerId == null || playerId.isBlank()) {
            return SupabaseResult.ok(0, Collections.emptyList());
        }
        String q = "select=player_id,quest_key,status"
                + "&player_id=eq." + enc(playerId.trim());
        SupabaseResult<List<NpcQuestRow>> r =
                doRead("npc_quests", q, LIST_NPC_QUEST, "fetchQuestsForPlayer");
        if (!r.ok() || r.value() == null) {
            return SupabaseResult.ok(r.httpStatus(), Collections.emptyList());
        }
        return r;
    }

    /* ==================================================================== */
    /*  Oracle (in-world answer-sign verb)                                  */
    /* ==================================================================== */

    /**
     * Read the OPEN puzzles to match an answer against — the world surface's half of the shared
     * resolver (ORACLE.md §5). Mirrors {@link #fetchActionableBeats}: same async/graceful contract,
     * returns an empty list on ANY failure (never null, never throws). Selects only the columns the
     * resolver needs; {@code accepted_answers} are spoilers held in service-role memory only.
     */
    public SupabaseResult<List<PuzzleRow>> fetchOpenPuzzles(int limit) {
        if (!config.isConfigured()) {
            return SupabaseResult.ok(0, Collections.emptyList());
        }
        String q = "select=puzzle_key,title,accepted_answers,outcome_type,outcome_payload,movement,active,max_attempts,requires_flags"
                + "&active=is.true"
                + "&order=movement.asc,created_at.asc"
                + "&limit=" + Math.max(1, limit);
        SupabaseResult<List<PuzzleRow>> r = doRead("puzzles", q, LIST_PUZZLE, "fetchOpenPuzzles");
        if (!r.ok() || r.value() == null) {
            return SupabaseResult.ok(r.httpStatus(), Collections.emptyList());
        }
        return r;
    }

    /** Recent solves for director-only progress/stuck reports. Spoiler-safe only because /obs is admin-gated. */
    public SupabaseResult<List<SolveReadRow>> fetchRecentSolves(int limit) {
        if (!config.isConfigured()) {
            return SupabaseResult.ok(0, Collections.emptyList());
        }
        String q = "select=puzzle_key,player_id,mc_uuid,discord_id,attempt_count,solved_at"
                + "&order=solved_at.desc"
                + "&limit=" + Math.max(1, Math.min(500, limit));
        SupabaseResult<List<SolveReadRow>> r = doRead("solves", q, LIST_SOLVE_READ, "fetchRecentSolves");
        if (!r.ok() || r.value() == null) {
            return SupabaseResult.ok(r.httpStatus(), Collections.emptyList());
        }
        return r;
    }

    /** Recent answer attempts for director-only stuck reports. Never shown to players. */
    public SupabaseResult<List<AnswerAttemptReadRow>> fetchRecentAnswerAttempts(int limit) {
        if (!config.isConfigured()) {
            return SupabaseResult.ok(0, Collections.emptyList());
        }
        String q = "select=puzzle_key,player_id,mc_uuid,discord_id,surface,raw,normalized,matched,at"
                + "&order=at.desc"
                + "&limit=" + Math.max(1, Math.min(500, limit));
        SupabaseResult<List<AnswerAttemptReadRow>> r =
                doRead("answer_attempts", q, LIST_ATTEMPT_READ, "fetchRecentAnswerAttempts");
        if (!r.ok() || r.value() == null) {
            return SupabaseResult.ok(r.httpStatus(), Collections.emptyList());
        }
        return r;
    }

    /** Authored hint rows for director-only stuck reports. Bodies are spoilers and must never be player-chat. */
    public SupabaseResult<List<HintRow>> fetchHints(int limit) {
        if (!config.isConfigured()) {
            return SupabaseResult.ok(0, Collections.emptyList());
        }
        String q = "select=puzzle_key,tier,body"
                + "&order=puzzle_key.asc,tier.asc"
                + "&limit=" + Math.max(1, Math.min(1000, limit));
        SupabaseResult<List<HintRow>> r = doRead("hints", q, LIST_HINT, "fetchHints");
        if (!r.ok() || r.value() == null) {
            return SupabaseResult.ok(r.httpStatus(), Collections.emptyList());
        }
        return r;
    }

    /**
     * Resolve a Minecraft {@code mc_uuid} to a {@link PlayerLookupRow} (players.id + discord_id), or
     * ok(null) if no such player / not configured. A solve keys on players.id, so the world surface
     * must do this lookup before writing {@code solves}. Never throws.
     */
    public SupabaseResult<PlayerLookupRow> fetchPlayerByUuid(String mcUuid) {
        if (!config.isConfigured() || mcUuid == null || mcUuid.isBlank()) {
            return SupabaseResult.ok(0, null);
        }
        String q = "select=id,mc_uuid,discord_id,observer_opt_out&mc_uuid=eq." + enc(mcUuid.trim()) + "&limit=1";
        SupabaseResult<List<PlayerLookupRow>> r =
                doRead("players", q, LIST_PLAYER_LOOKUP, "fetchPlayerByUuid");
        if (!r.ok()) return SupabaseResult.fail(r.httpStatus(), r.error());
        List<PlayerLookupRow> list = r.value();
        PlayerLookupRow first = (list == null || list.isEmpty()) ? null : list.get(0);
        return SupabaseResult.ok(r.httpStatus(), first);
    }

    /**
     * Consent floor for Observer capture. Returns true when the player has opted out, cannot be found, or
     * Supabase cannot be read. Capture is privacy-sensitive, so uncertainty means skip before storage.
     */
    public boolean observerOptedOut(String mcUuid) {
        SupabaseResult<PlayerLookupRow> r = fetchPlayerByUuid(mcUuid);
        if (!r.ok() || r.value() == null) return true;
        return !Boolean.FALSE.equals(r.value().observerOptOut);
    }

    /**
     * How many answer_attempts a given {@code mc_uuid} has made since {@code sinceIso} — the durable
     * (cross-restart) half of the rate-limit (the in-memory {@link com.observance.watcher.util.RateLimiter}
     * is the cheap first gate). Returns -1 on any failure so the caller can choose to fail-OPEN to the
     * in-memory limiter rather than wrongly block a legitimate keeper on a DB hiccup.
     */
    public int countWorldAttemptsSince(String mcUuid, String sinceIso) {
        return countWorldAttemptsSince(mcUuid, sinceIso, null);
    }

    /**
     * As {@link #countWorldAttemptsSince(String, String)} but optionally scoped to a single
     * {@code puzzleKey} — used for the per-puzzle {@code max_attempts} cap so one puzzle's cap counts
     * ONLY tries on that puzzle (mirrors the bot's {@code countRecentAttempts({puzzleKey})}). A high
     * explicit {@code limit} is sent so a busy window isn't silently truncated by PostgREST's default
     * page size (which would under-count the gate). Returns -1 on any failure (caller fails OPEN).
     */
    public int countWorldAttemptsSince(String mcUuid, String sinceIso, String puzzleKey) {
        if (!config.isConfigured() || mcUuid == null || mcUuid.isBlank() || sinceIso == null) {
            return -1;
        }
        String q = "select=id&surface=eq.world&mc_uuid=eq." + enc(mcUuid.trim())
                + "&at=gte." + enc(sinceIso)
                + "&limit=10000";
        if (puzzleKey != null && !puzzleKey.isBlank()) {
            q += "&puzzle_key=eq." + enc(puzzleKey.trim());
        }
        // HEAD with Prefer: count=exact returns the count in Content-Range; simpler here to read ids.
        SupabaseResult<List<PlayerLookupRow>> r =
                doRead("answer_attempts", q, LIST_PLAYER_LOOKUP, "countWorldAttemptsSince");
        if (!r.ok() || r.value() == null) return -1;
        return r.value().size();
    }

    /**
     * Append an answer_attempts audit row (matched or not). Fire-and-forget durability: on failure it
     * is queued (bounded) like other writes. Never throws.
     */
    public SupabaseResult<Void> insertAnswerAttempt(
            com.observance.watcher.data.rows.AnswerAttemptRow row) {
        if (row == null) return SupabaseResult.fail(0, "null-row");
        String body = gson.toJson(row);
        if (!config.isConfigured()) {
            enqueue("insertAnswerAttempt", () ->
                    doWrite("POST", "answer_attempts", "", body, false, "insertAnswerAttempt"));
            return SupabaseResult.queued();
        }
        SupabaseResult<Void> r =
                doWrite("POST", "answer_attempts", "", body, false, "insertAnswerAttempt");
        if (!r.ok()) {
            enqueue("insertAnswerAttempt", () ->
                    doWrite("POST", "answer_attempts", "", body, false, "insertAnswerAttempt"));
        }
        return r;
    }

    /**
     * The IDEMPOTENT solve guard. INSERT into {@code solves} with {@code ignore-duplicates} against
     * {@code unique(puzzle_key, player_id)} and {@code return=representation}, so:
     * <ul>
     *   <li>a genuinely-new solve → 201 with a one-row body → returns {@link SolveOutcome#NEW};</li>
     *   <li>an already-solved conflict → 200/201 with an EMPTY body → {@link SolveOutcome#DUPLICATE};</li>
     *   <li>any network/HTTP/parse failure → {@link SolveOutcome#FAILED} (caller withholds, enqueues
     *       NOTHING — never grant a reward we couldn't durably guard).</li>
     * </ul>
     * Only a {@code NEW} result may proceed to enqueue the beat — this is what stops a double-fire.
     * MUST be called from an async thread (it blocks on I/O).
     */
    public SolveOutcome insertSolveIfNew(com.observance.watcher.data.rows.SolveRow row) {
        if (!config.isConfigured() || row == null
                || row.puzzleKey == null || row.playerId == null) {
            return SolveOutcome.FAILED;
        }
        String body = gson.toJson(row);
        return withRetries("insertSolveIfNew", () -> {
            HttpRequest req = baseRequest("solves", "")
                    .header("Content-Type", "application/json")
                    // ignore-duplicates → conflict is NOT an error; representation → see if a row came back.
                    .header("Prefer", "resolution=ignore-duplicates,return=representation")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();
            if (code >= 200 && code < 300) {
                markSuccess();
                String b = resp.body();
                boolean inserted = b != null && b.trim().length() > 2; // "[]" → conflict, "[{…}]" → new
                return inserted ? SolveOutcome.NEW : SolveOutcome.DUPLICATE;
            }
            markFailure();
            return SolveOutcome.FAILED;
        }, SolveOutcome.FAILED);
    }

    /** Outcome of an idempotent solve insert. */
    public enum SolveOutcome { NEW, DUPLICATE, FAILED }

    /**
     * Read-only check: does {@code playerId} already have a {@code solves} row for
     * {@code puzzleKey}? Used by {@link com.observance.watcher.oracle.OracleResolver} to pick the
     * correct candidate when a normalized answer is legitimately shared by more than one open
     * puzzle (a sequenced pair — an already-solved upstream owner + its freshly-open downstream
     * consumer), so a keeper resubmitting the same phrase at the new gate isn't shadowed by the
     * old one forever. Fails OPEN (returns {@code false}) on any read error or when unconfigured —
     * worst case a genuinely-solved candidate is retried through {@link #insertSolveIfNew}, which
     * safely no-ops on the real conflict (returns {@code DUPLICATE}), so failing open here never
     * grants a duplicate reward. MUST be called from an async thread (it blocks on I/O).
     */
    public boolean hasSolvedWorld(String puzzleKey, String playerId) {
        if (!config.isConfigured() || puzzleKey == null || puzzleKey.isBlank()
                || playerId == null || playerId.isBlank()) {
            return false;
        }
        String q = "select=id&puzzle_key=eq." + enc(puzzleKey.trim())
                + "&player_id=eq." + enc(playerId.trim()) + "&limit=1";
        SupabaseResult<List<PlayerLookupRow>> r = doRead("solves", q, LIST_PLAYER_LOOKUP, "hasSolvedWorld");
        if (!r.ok() || r.value() == null) return false;
        return !r.value().isEmpty();
    }

    /**
     * Enqueue a new {@code beat_queue} beat (the oracle is the producer). Defaults status to
     * 'approved' when unset so player-earned unlocks fire on the next poll without a human gate.
     * On failure the insert is queued (bounded). Never throws.
     */
    public SupabaseResult<Void> insertBeat(
            com.observance.watcher.data.rows.BeatQueueInsertRow row) {
        if (row == null || row.type == null || row.type.isBlank()) {
            return SupabaseResult.fail(0, "null-or-typeless-beat");
        }
        if (row.status == null || row.status.isBlank()) {
            row.status = "approved"; // player-earned reward: no human gate
        }
        String body = gson.toJson(row);
        if (!config.isConfigured()) {
            enqueue("insertBeat", () ->
                    doWrite("POST", "beat_queue", "", body, false, "insertBeat"));
            return SupabaseResult.queued();
        }
        SupabaseResult<Void> r = doWrite("POST", "beat_queue", "", body, false, "insertBeat");
        if (!r.ok()) {
            enqueue("insertBeat", () ->
                    doWrite("POST", "beat_queue", "", body, false, "insertBeat"));
        }
        return r;
    }

    /**
     * ATOMICALLY merge keys into {@code arc_state.flags} via the {@code observance_merge_arc_flags}
     * RPC (0006_requires_flags.sql) — {@code flags = flags || p_flags} in ONE server-side statement,
     * never read-modify-write. This is the world surface's twin of the bot's {@code setArcFlags}; an
     * in-world solve of a flag-setting puzzle (e.g. the Iss catch setting {@code iss_caught}) advances
     * the arc identically on both surfaces (OVERHAUL.md §3; closes will-it-run #12). Keep the flags
     * object FLAT (the merge is shallow). On failure the call is queued (bounded). Never throws.
     */
    public SupabaseResult<Void> mergeArcFlags(com.google.gson.JsonObject flags) {
        if (flags == null || flags.size() == 0) {
            return SupabaseResult.ok(0, null);
        }
        com.google.gson.JsonObject args = new com.google.gson.JsonObject();
        args.add("p_flags", flags);
        String body = gson.toJson(args);
        // PostgREST exposes a function at /rpc/<name>; baseRequest prefixes the REST base URL.
        if (!config.isConfigured()) {
            enqueue("mergeArcFlags", () ->
                    doWrite("POST", "rpc/observance_merge_arc_flags", "", body, false, "mergeArcFlags"));
            return SupabaseResult.queued();
        }
        SupabaseResult<Void> r =
                doWrite("POST", "rpc/observance_merge_arc_flags", "", body, false, "mergeArcFlags");
        if (!r.ok()) {
            enqueue("mergeArcFlags", () ->
                    doWrite("POST", "rpc/observance_merge_arc_flags", "", body, false, "mergeArcFlags"));
        }
        return r;
    }

    /* ==================================================================== */
    /*  World paste ledger (FAWE single-paste idempotency, backlog D10)      */
    /* ==================================================================== */

    /**
     * The DURABLE (cross-restart) single-paste guard for a large FAWE set-piece (BUILD-MANIFEST §6 /
     * INTEGRATION-V2 D10). INSERT one {@code world_paste_ledger} row keyed by the UNIQUE
     * {@code (world, site_id, schematic, base_x, base_y, base_z)} tuple, exactly like
     * {@link #insertSolveIfNew} guards {@code solves}:
     * <ul>
     *   <li>a genuinely-new claim → 2xx with a one-row body → {@link PasteClaim#NEW} (proceed to paste);</li>
     *   <li>an already-pasted footprint → 2xx with an EMPTY body → {@link PasteClaim#DUPLICATE} (skip);</li>
     *   <li>any network/HTTP/parse failure → {@link PasteClaim#FAILED} (skip — never double-paste a
     *       set-piece we couldn't durably guard; the in-process applied-set + footprint occupancy still
     *       cover the common re-fire, this is only the durable backstop).</li>
     * </ul>
     * The ledger governs SINGLE-PASTE set-pieces ONLY; the M-III A→B swap rides {@code RoomSwapBeat}'s
     * {@code swapped} PDC marker, NOT this table (no double-guard — BUILD-MANIFEST §8 / BP0-3).
     *
     * <p>Blocking I/O → MUST be called from an async thread (mirrors {@link #insertSolveIfNew}). When the
     * client is not configured we cannot durably guard, so we return {@link PasteClaim#NEW} and let the
     * footprint sweep be the sole guard (dev/offline parity — never silently swallow the set-piece).
     */
    public PasteClaim claimPasteLedger(String world, String siteId, String schematic,
                                       int baseX, int baseY, int baseZ) {
        if (world == null || world.isBlank() || schematic == null || schematic.isBlank()) {
            return PasteClaim.FAILED;
        }
        if (!config.isConfigured()) {
            // No durable store reachable — defer entirely to the footprint sweep (cannot block the build).
            return PasteClaim.NEW;
        }
        String sid = (siteId == null) ? "" : siteId;
        String body = "{\"world\":\"" + jsonEscape(world)
                + "\",\"site_id\":\"" + jsonEscape(sid)
                + "\",\"schematic\":\"" + jsonEscape(schematic)
                + "\",\"base_x\":" + baseX
                + ",\"base_y\":" + baseY
                + ",\"base_z\":" + baseZ + "}";
        return withRetries("claimPasteLedger", () -> {
            HttpRequest req = baseRequest("world_paste_ledger", "")
                    .header("Content-Type", "application/json")
                    // ignore-duplicates → a UNIQUE-conflict is NOT an error; representation → see if a row came back.
                    .header("Prefer", "resolution=ignore-duplicates,return=representation")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();
            if (code >= 200 && code < 300) {
                markSuccess();
                String b = resp.body();
                boolean inserted = b != null && b.trim().length() > 2; // "[]" → conflict, "[{…}]" → new
                return inserted ? PasteClaim.NEW : PasteClaim.DUPLICATE;
            }
            markFailure();
            return PasteClaim.FAILED;
        }, PasteClaim.FAILED);
    }

    /** Outcome of an idempotent {@code world_paste_ledger} claim (single-paste guard). */
    public enum PasteClaim { NEW, DUPLICATE, FAILED }

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
                logFailure(description, "offline queue full (max " + config.offlineQueueMax()
                        + ") — dropped the oldest queued write, data lost");
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
                    logFailure(ctx, "parse-error (http " + code + ")");
                    return SupabaseResult.<List<T>>fail(code, "parse-error");
                }
                markSuccess();
                return SupabaseResult.ok(code, parsed == null ? Collections.<T>emptyList() : parsed);
            }
            markFailure();
            logFailure(ctx, "http-" + code);
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
            logFailure(ctx, "http-" + code);
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
                    // Last attempt failed with an exception — this call is truly lost; report it.
                    logFailure(ctx, "exhausted after " + attempts + " attempt(s): "
                            + t.getClass().getSimpleName()
                            + (t.getMessage() == null ? "" : (": " + t.getMessage())));
                    return fallback;
                }
            }
        }
        return fallback;
    }

    /** The one place every Supabase failure funnels through — so a bad write is never silent. */
    private void logFailure(String ctx, String detail) {
        try {
            logger.log(Level.WARNING, "[supabase] " + ctx + ": " + detail);
        } catch (Throwable ignored) {
            // Logging must never itself throw into a caller that's already handling a failure.
        }
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
