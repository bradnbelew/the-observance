package com.observance.watcher.oracle;

import com.google.gson.JsonObject;
import com.observance.watcher.config.ObservanceConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.SupabaseResult;
import com.observance.watcher.data.rows.AnswerAttemptRow;
import com.observance.watcher.data.rows.ArcStateRow;
import com.observance.watcher.data.rows.BeatQueueInsertRow;
import com.observance.watcher.data.rows.PlayerLookupRow;
import com.observance.watcher.data.rows.PuzzleRow;
import com.observance.watcher.data.rows.SolveRow;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The WORLD-surface half of the shared oracle resolver (ORACLE.md §1, §5). Given a normalized answer
 * from an in-world answer-sign, it does — entirely off the main thread, fully fault-isolated — the
 * SAME thing the bot does on the Discord surface:
 *
 * <ol>
 *   <li>gibberish guard: empty normalized → silent, not even logged;</li>
 *   <li>rate-limit (in-memory token bucket + cooldown, then a durable answer_attempts window) →
 *       WITHHELD without a tell;</li>
 *   <li>match the whole normalized string by exact set-membership against the OPEN puzzles;</li>
 *   <li>log every plausible attempt (matched bool only — never which part);</li>
 *   <li>on a match, resolve mc_uuid → players.id, then INSERT solves with the idempotency guard;
 *       only a genuinely-NEW solve enqueues the reward beat (status 'approved' → fires next poll).</li>
 * </ol>
 *
 * <p>The result is intentionally low-information: the in-world "voice" is the enqueued beat itself
 * (a door, a sound, a structure), so a miss/withheld/duplicate produces NO world feedback — silence,
 * never an error and never a closeness tell.
 *
 * <p>This class touches NO Bukkit objects, so it is safe to run on an async thread. It is reusable by
 * any future world-surface verb (a command, an interact) — only the entry listener differs.
 */
public final class OracleResolver {

    /** What happened, for the caller's logging only (never surfaced to the player as a tell). */
    public enum Result {
        /** Empty/gibberish input — ignored entirely. */
        IGNORED,
        /** Rate-limited or per-puzzle cap reached — withheld, no tell. */
        WITHHELD,
        /** Plausible answer, matched no open puzzle — logged, silent. */
        MISS,
        /** Matched, but this keeper already solved it — silent (no double-fire). */
        ALREADY_SOLVED,
        /** First solve — outcome applied; a reward beat may have been enqueued. */
        SOLVED,
        /** A graceful degrade (DB outage / unknown player) — withheld silently. */
        UNAVAILABLE
    }

    private final ObservanceConfig config;
    private final SupabaseClient supabase;
    private final RateLimiter rateLimiter;
    private final Safety safety;

    public OracleResolver(ObservanceConfig config, SupabaseClient supabase,
                          RateLimiter rateLimiter, Safety safety) {
        this.config = config;
        this.supabase = supabase;
        this.rateLimiter = rateLimiter;
        this.safety = safety;
    }

    /**
     * Resolve a raw in-world answer for the keeper identified by {@code mcUuid} / {@code playerName}.
     * {@code boundPuzzleKey} optionally restricts matching to a single puzzle (a focused answer-sign
     * site); pass null to match against ALL open puzzles (the non-linear web default).
     * Blocking I/O — call ONLY from an async thread. Never throws (wrapped in Safety internally).
     */
    public Result resolveWorld(String mcUuid, String playerName, String raw, String boundPuzzleKey) {
        return safety.call("oracle.resolveWorld",
                () -> resolveWorldInner(mcUuid, playerName, raw, boundPuzzleKey), Result.UNAVAILABLE);
    }

    private Result resolveWorldInner(String mcUuid, String playerName, String raw, String boundPuzzleKey) {
        // 1. Gibberish guard — an empty normalized form is not "plausibly an answer". Silent, unlogged.
        final String norm = AnswerNormalizer.normalize(raw);
        if (norm.isEmpty()) {
            return Result.IGNORED;
        }
        if (mcUuid == null || mcUuid.isBlank()) {
            return Result.UNAVAILABLE;
        }

        // 2a. In-memory rate-limit (cheap, offline-safe): a cooldown AND a token bucket per keeper.
        //     Either gate tripping = WITHHELD. Anti-brute-force on a short answer.
        String rlKey = "oracle:" + mcUuid;
        long cooldownMs = config.oracleCooldownSeconds() * 1000L;
        boolean cdOk = cooldownMs <= 0 || rateLimiter.tryCooldown(rlKey + ":cd", cooldownMs);
        boolean tokenOk = rateLimiter.tryToken(rlKey + ":tok",
                config.oracleBurst(), config.oracleRefillSeconds() * 1000L);
        if (!cdOk || !tokenOk) {
            return Result.WITHHELD;
        }

        // 2b. Durable rate-limit window (cross-restart): cap attempts in the configured window. On a
        //     DB read failure (-1) we DON'T block — the in-memory gate above already covers the
        //     common case, and we never wrongly silence a legitimate keeper on a hiccup (fail-open).
        int windowSeconds = Math.max(config.oracleRefillSeconds(), config.oracleCooldownSeconds());
        if (windowSeconds > 0) {
            String sinceIso = Instant.now().minus(windowSeconds, ChronoUnit.SECONDS).toString();
            int recent = supabase.countWorldAttemptsSince(mcUuid, sinceIso);
            // A generous durable ceiling = burst capacity; the bucket is the fine-grained gate.
            int durableCeiling = Math.max(config.oracleBurst() * 4, config.oracleBurst());
            if (recent >= durableCeiling) {
                return Result.WITHHELD;
            }
        }

        // 3. Fetch OPEN puzzles + match by exact whole-string set-membership. Collect ALL matching
        //    candidates, not just the first — a normalized answer may legitimately be shared by a
        //    SEQUENCED pair (an already-solved upstream owner that stays open + its freshly-open
        //    downstream consumer, e.g. the bound word on stone-iss-wall/bound-word, then again on
        //    iss-bound-word-callback). Picking only the first would permanently shadow the second.
        SupabaseResult<List<PuzzleRow>> pr = supabase.fetchOpenPuzzles(200);
        if (!pr.ok() || pr.value() == null) {
            // Can't read the web → degrade silently (don't even log an attempt we couldn't evaluate).
            return Result.UNAVAILABLE;
        }
        // The storylet gate: load arc_state.flags once and AND-test each row's requires_flags, so a
        // gated row (the M4 Iss chain, the Seventh deep) is invisible in-world until its upstream door
        // set the flag — identical to the Discord getOpenPuzzles gate (FlagGate is its proven twin).
        Map<String, Object> arcFlags = fetchArcFlags();
        List<PuzzleRow> candidates = allMatches(pr.value(), norm, boundPuzzleKey, arcFlags);

        // 4. Resolve the keeper id (needed for both the attempt log and the solve guard).
        PlayerLookupRow keeper = lookupKeeper(mcUuid);
        String playerId = keeper == null ? null : keeper.id;
        String discordId = keeper == null ? null : keeper.discordId;

        // 5. Log the attempt (matched bool only — NEVER which part). Best-effort, queued on failure.
        //    Attributed to the first candidate, mirroring the bot's resolve.ts (logAttempt uses
        //    candidates[0] regardless of which candidate is ultimately picked below).
        logAttempt(candidates.isEmpty() ? null : candidates.get(0).puzzleKey, playerId, mcUuid, discordId,
                raw, norm, !candidates.isEmpty());

        if (candidates.isEmpty()) {
            return Result.MISS; // plausible but wrong → silence (contrast: a dead_end is HEARD)
        }

        if (playerId == null) {
            // A solve must be a known keeper; without an id we cannot durably guard it → withhold.
            return Result.UNAVAILABLE;
        }

        // 5b. Among the candidates, pick the first this keeper has NOT already solved — so an
        //     already-done upstream owner never shadows its downstream re-submission (mirrors the
        //     bot's pickCandidate in resolve.ts). If every candidate is already solved, `matched`
        //     falls back to the first (the canonical owner) and alreadySolved stays true.
        PuzzleRow matched = candidates.get(0);
        boolean alreadySolved = true;
        for (PuzzleRow c : candidates) {
            if (!supabase.hasSolvedWorld(c.puzzleKey, playerId)) {
                matched = c;
                alreadySolved = false;
                break;
            }
        }

        // Per-puzzle attempt cap (on top of the global limiter): reaching it = withheld, never a hint.
        // Scope to THIS puzzle and the SAME window the bot uses (countRecentAttempts is windowed, not
        // all-time) so the two surfaces behave identically. SEMANTICS: exactly max_attempts tries are
        // allowed, the next is withheld — the bot counts PRIOR attempts then compares `prior >= max`.
        // Here we already logged the current attempt at step 5, so subtract 1 to get the prior count,
        // making the boundary identical to the bot (allow 1..max, withhold max+1 onward).
        if (matched.maxAttempts != null && matched.maxAttempts > 0) {
            int windowSecs = Math.max(config.oracleRefillSeconds(), config.oracleCooldownSeconds());
            String capSinceIso = Instant.now().minus(Math.max(1, windowSecs), ChronoUnit.SECONDS).toString();
            int counted = supabase.countWorldAttemptsSince(mcUuid, capSinceIso, matched.puzzleKey);
            if (counted >= 0) {
                int prior = Math.max(0, counted - 1); // exclude the attempt we just logged at step 5
                if (prior >= matched.maxAttempts) {
                    return Result.WITHHELD;
                }
            }
        }

        // 6b. Every candidate already solved (the pick loop found none new) → silent, no re-fire —
        //     matches the bot's step 7 (checked AFTER the cap, same order, so a rate-limited replay
        //     still withholds rather than confirming "yes, you solved this").
        if (alreadySolved) {
            return Result.ALREADY_SOLVED;
        }

        // 7. IDEMPOTENT solve guard BEFORE any reward. Only a genuinely-new solve proceeds. (A DUPLICATE
        //    here means the pick raced a concurrent solve between the read at 5b and this write.)
        SolveRow solveRow = new SolveRow(matched.puzzleKey, playerId, mcUuid, discordId, 1);
        SupabaseClient.SolveOutcome outcome = supabase.insertSolveIfNew(solveRow);
        switch (outcome) {
            case DUPLICATE -> { return Result.ALREADY_SOLVED; }   // lost the race → silent, no re-fire
            case FAILED -> { return Result.UNAVAILABLE; }         // couldn't guard → grant nothing
            case NEW -> { /* first solve — fall through to apply the outcome */ }
        }

        // 7. Apply the outcome. The world-surface "voice" is the beat; dead_end/lore simply enqueue
        //    nothing (the solve row already recorded it, so it can't be farmed). next_clue/side_quest/
        //    main_beat enqueue their authored beat (status 'approved' → fires on the next poll).
        applyOutcome(matched, mcUuid);
        return Result.SOLVED;
    }

    /* ------------------------------------------------------------------ */
    /* matching                                                            */
    /* ------------------------------------------------------------------ */

    /**
     * ALL OPEN puzzles whose accepted_answers contain the normalized string (ORACLE.md §2), among the
     * rows the storylet gate leaves open, in the plugin's natural fetch order (movement asc,
     * created_at asc). When {@code boundPuzzleKey} is non-null, only that puzzle is considered (a
     * focused answer-sign site). Usually 0 or 1 result; more than 1 only for a plaintext legitimately
     * shared by a SEQUENCED pair (an already-solved upstream owner that stays open + its freshly-open
     * downstream consumer) — the caller picks the first the keeper has NOT already solved (mirrors the
     * bot's {@code pickCandidate} in resolve.ts; this world-surface path used to pick the first match
     * unconditionally, which permanently shadowed a downstream row sharing an upstream's plaintext). A
     * row is skipped unless every key in its {@code requires_flags} is truthy in {@code arcFlags} (the
     * SAME predicate as the bot, via {@link FlagGate}).
     */
    private List<PuzzleRow> allMatches(List<PuzzleRow> puzzles, String norm, String boundPuzzleKey,
                                 Map<String, Object> arcFlags) {
        List<PuzzleRow> out = new ArrayList<>();
        for (PuzzleRow p : puzzles) {
            if (p == null || p.acceptedAnswers == null) continue;
            if (!Boolean.TRUE.equals(p.active)) continue; // defensive: the query already filters
            // storylet gate — skip a row whose requires_flags are not all satisfied by arc_state.flags.
            if (!FlagGate.satisfied(p.requiresFlagsMap(), arcFlags)) continue;
            if (boundPuzzleKey != null && !boundPuzzleKey.equals(p.puzzleKey)) continue;
            for (String a : p.acceptedAnswers) {
                // accepted_answers are stored already-normalized; compare verbatim.
                if (a != null && a.equals(norm)) {
                    out.add(p);
                    break;
                }
            }
        }
        return out;
    }

    /**
     * Load {@code arc_state.flags} as a flat {@code Map<String,Object>} for the gate. Graceful: any
     * miss/outage returns empty, which keeps every gated row CLOSED (the safe failure — never leak a
     * gated row because the flags read stumbled). Never throws.
     */
    private Map<String, Object> fetchArcFlags() {
        try {
            SupabaseResult<ArcStateRow> r = supabase.fetchArcState();
            if (r.ok() && r.value() != null) {
                return r.value().flagsMap();
            }
        } catch (Throwable ignored) {
            // fall through to empty — gated rows stay closed
        }
        return Collections.emptyMap();
    }

    /* ------------------------------------------------------------------ */
    /* outcome → beat                                                      */
    /* ------------------------------------------------------------------ */

    /**
     * Enqueue the authored reward beat, if any. Reads {@code outcome_payload.beat}; resolves the
     * {@code "{solver}"} mc_uuid placeholder to the real solver; defaults status to 'approved'. A
     * {@code dead_end}/{@code lore} row (no {@code beat}) enqueues nothing — and that is correct.
     */
    private void applyOutcome(PuzzleRow matched, String solverUuid) {
        JsonObject outcome = matched.outcomeObject();
        if (outcome == null) return;

        // (a) set_flags — apply the atomic merge FIRST, BEFORE the beat-less early return, so a
        //     flag-setting lore/dead_end/next_clue row advances the arc even with no beat. This is the
        //     world surface's twin of the bot resolver's applyOutcome step (a); without it an in-world
        //     solve of a flag-setting puzzle (the Iss catch → iss_caught) would NOT open the back half
        //     and the two surfaces would diverge (closes will-it-run #12).
        com.google.gson.JsonElement setFlagsEl = outcome.get("set_flags");
        if (setFlagsEl != null && setFlagsEl.isJsonObject() && setFlagsEl.getAsJsonObject().size() > 0) {
            supabase.mergeArcFlags(setFlagsEl.getAsJsonObject());
        }

        // (b) the optional in-world reward beat.
        com.google.gson.JsonElement beatEl = outcome.get("beat");
        if (beatEl == null || !beatEl.isJsonObject()) {
            return; // no in-world reward (dead_end / lore / a beat-less next_clue) — nothing to enqueue
        }
        JsonObject beat = beatEl.getAsJsonObject();

        String type = optString(beat, "type", "unlock");
        String mcUuid = optString(beat, "mc_uuid", null);
        if (mcUuid != null && mcUuid.equals("{solver}")) {
            mcUuid = solverUuid; // resolve placeholder → the keeper who solved it
        }
        String siteId = optString(beat, "site_id", null);
        Integer priority = optInt(beat, "priority");
        String status = optString(beat, "status", "approved"); // player reward → fires immediately

        com.google.gson.JsonElement payloadEl = beat.get("payload");
        com.google.gson.JsonElement payload =
                (payloadEl != null && payloadEl.isJsonObject()) ? payloadEl : new JsonObject();

        BeatQueueInsertRow row = new BeatQueueInsertRow(type, status, mcUuid, siteId, priority, payload);
        SupabaseResult<Void> r = supabase.insertBeat(row);
        if (config.debug()) {
            safety.info("oracle.enqueue",
                    "puzzle=" + matched.puzzleKey + " type=" + type + " result=" + r);
        }
    }

    /* ------------------------------------------------------------------ */
    /* helpers                                                             */
    /* ------------------------------------------------------------------ */

    private PlayerLookupRow lookupKeeper(String mcUuid) {
        SupabaseResult<PlayerLookupRow> r = supabase.fetchPlayerByUuid(mcUuid);
        return (r.ok() && r.value() != null) ? r.value() : null;
    }

    /** Defensive bound on stored raw input (parity with the bot's MAX_RAW_LEN). Signs are already
     *  short, but never store an unbounded string in answer_attempts.raw. */
    private static final int MAX_RAW_LEN = 512;

    private void logAttempt(String puzzleKey, String playerId, String mcUuid, String discordId,
                            String raw, String norm, boolean matched) {
        String rawCapped = (raw != null && raw.length() > MAX_RAW_LEN)
                ? raw.substring(0, MAX_RAW_LEN) : raw;
        AnswerAttemptRow row = new AnswerAttemptRow(
                puzzleKey, playerId, mcUuid, discordId, "world", rawCapped, norm, matched);
        supabase.insertAnswerAttempt(row);
    }

    private static String optString(JsonObject o, String key, String def) {
        try {
            com.google.gson.JsonElement e = o.get(key);
            if (e != null && e.isJsonPrimitive()) {
                String s = e.getAsString();
                return s == null ? def : s;
            }
        } catch (Throwable ignored) { }
        return def;
    }

    private static Integer optInt(JsonObject o, String key) {
        try {
            com.google.gson.JsonElement e = o.get(key);
            if (e != null && e.isJsonPrimitive()) return e.getAsInt();
        } catch (Throwable ignored) { }
        return null;
    }
}
