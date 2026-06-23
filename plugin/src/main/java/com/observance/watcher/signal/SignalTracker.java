package com.observance.watcher.signal;

import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.rows.BaseRow;
import com.observance.watcher.data.rows.CustomComplianceRow;
import com.observance.watcher.data.rows.DossierRow;
import com.observance.watcher.data.rows.HeatmapCellRow;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Signal Tracker service (DESIGN §2.1 + §2.2) — the dossier brain.
 *
 * <p>Owns the in-memory per-player {@link PlayerSignals}, the {@link HeatmapAccumulator}, and the
 * {@link BaseDetector}. Listeners and samplers call its {@code track*} / accumulator hooks on the
 * MAIN thread (cheap). A scheduled FLUSH task (async) writes the dirty aggregates to Supabase via
 * {@link SupabaseClient} — batched + defensive, never blocking the main thread.
 *
 * <p>This is PURE TRACKING. It produces NO world effects. Downstream engines (beat engine, custom
 * engine, scalpel) consume read-only {@link SignalSnapshot}s via {@link #snapshot(UUID)} /
 * {@link #snapshotByName} and the heatmap/base data in Supabase. The tracker decides nothing about
 * what happens in-world — it only measures.
 *
 * <h2>Threading</h2>
 * <ul>
 *   <li>{@code track*} accumulators: MAIN thread (called from listeners). Cheap, lock-light.</li>
 *   <li>{@link #flushOnce()}: ASYNC (does network I/O). Reads consistent snapshots under locks,
 *       never touches Bukkit.</li>
 * </ul>
 * Every public method is null-safe and Safety-friendly; a single bad row never aborts the batch.
 */
public final class SignalTracker {

    private final SupabaseClient supabase;
    private final Scheduler scheduler;
    private final Safety safety;
    private volatile TrackerConfig cfg;

    private final Map<UUID, PlayerSignals> players = new ConcurrentHashMap<>();
    // name → uuid index for name-based lookups (e.g. chat/report targeting). Lower-cased.
    private final Map<String, UUID> nameIndex = new ConcurrentHashMap<>();

    private final HeatmapAccumulator heatmap;
    private final BaseDetector baseDetector;

    public SignalTracker(SupabaseClient supabase, Scheduler scheduler, Safety safety,
                         TrackerConfig cfg, int heatmapCellSize) {
        this.supabase = supabase;
        this.scheduler = scheduler;
        this.safety = safety;
        this.cfg = cfg == null ? TrackerConfig.defaults() : cfg;
        this.heatmap = new HeatmapAccumulator(heatmapCellSize, 50_000);
        this.baseDetector = new BaseDetector(this.cfg);
    }

    /* ================================================================== */
    /*  Registry                                                          */
    /* ================================================================== */

    /** Get-or-create the signals for a player. Updates the name index. Never null. */
    public PlayerSignals signals(UUID uuid, String name) {
        if (uuid == null) return new PlayerSignals(new UUID(0, 0), name); // detached, never indexed
        PlayerSignals ps = players.computeIfAbsent(uuid, u -> new PlayerSignals(u, name));
        if (name != null && !name.isBlank()) {
            ps.setName(name);
            nameIndex.put(name.toLowerCase(java.util.Locale.ROOT), uuid);
        }
        return ps;
    }

    /** Existing signals for a uuid, or null if untracked. */
    public PlayerSignals get(UUID uuid) {
        return uuid == null ? null : players.get(uuid);
    }

    /** Mark a player's session start (reset session flags). Safe to call on join. */
    public void onJoin(UUID uuid, String name) {
        PlayerSignals ps = signals(uuid, name);
        ps.onSessionStart();
    }

    /** On quit, nothing is dropped — counters persist; we just flush this player eagerly. */
    public void onQuit(UUID uuid) {
        if (uuid == null) return;
        PlayerSignals ps = players.get(uuid);
        if (ps == null) return;
        scheduler.runAsyncSafe("tracker.flushOnQuit", () -> flushPlayer(ps));
    }

    public HeatmapAccumulator heatmap() { return heatmap; }
    public BaseDetector baseDetector() { return baseDetector; }
    public TrackerConfig config() { return cfg; }

    /** Swap in a new config (on reload). The base detector reads the live floor each pass. */
    public void setConfig(TrackerConfig newCfg) {
        if (newCfg != null) this.cfg = newCfg;
    }

    /* ================================================================== */
    /*  READ side — the surface downstream engines consume                */
    /* ================================================================== */

    /** Immutable snapshot for a uuid, or null if the player is untracked. */
    public SignalSnapshot snapshot(UUID uuid) {
        PlayerSignals ps = get(uuid);
        return ps == null ? null : ps.snapshot();
    }

    /** Immutable snapshot by (case-insensitive) name, or null if unknown. */
    public SignalSnapshot snapshotByName(String name) {
        if (name == null) return null;
        UUID id = nameIndex.get(name.toLowerCase(java.util.Locale.ROOT));
        return id == null ? null : snapshot(id);
    }

    /** Snapshots for all tracked players (consistent per-player copies). */
    public List<SignalSnapshot> allSnapshots() {
        List<SignalSnapshot> out = new java.util.ArrayList<>(players.size());
        for (PlayerSignals ps : players.values()) out.add(ps.snapshot());
        return out;
    }

    public int trackedPlayerCount() { return players.size(); }

    /* ================================================================== */
    /*  FLUSH — async, batched, defensive                                 */
    /* ================================================================== */

    /**
     * Flush all dirty dossiers + changed compliance + heatmap deltas. ASYNC ONLY. Each write is
     * independently fault-isolated: one failure is logged + (by the client) queued, the rest
     * proceed. Returns a small summary for logging.
     */
    public FlushSummary flushOnce() {
        int dossiers = 0, compliance = 0, cells = 0;

        // --- dossiers + compliance, per dirty player ---
        for (PlayerSignals ps : players.values()) {
            try {
                if (ps.isDossierDirty()) {
                    if (flushPlayer(ps)) dossiers++;
                }
                compliance += flushCompliance(ps);
            } catch (Throwable t) {
                safety.warn("tracker.flush.player", describe(t));
            }
        }

        // --- heatmap cells ---
        cells = flushHeatmap();

        return new FlushSummary(dossiers, compliance, cells);
    }

    /** Flush one player's dossier (absolute snapshot). Returns true if a write was attempted. */
    private boolean flushPlayer(PlayerSignals ps) {
        if (ps == null) return false;
        SignalSnapshot s;
        try {
            s = ps.snapshot();
        } catch (Throwable t) {
            safety.warn("tracker.snapshot", describe(t));
            return false;
        }
        DossierRow row = toDossierRow(s);
        // Optimistically clear the dirty flag BEFORE the write so concurrent bumps re-dirty it;
        // the client queues failed writes internally, so we won't silently lose data on outage.
        ps.clearDossierDirty();
        var res = supabase.upsertDossier(row);
        if (!res.ok() && (res.error() == null || !"queued-offline".equals(res.error()))) {
            // Hard failure that wasn't queued by the client — re-mark dirty so we retry next
            // cycle (the snapshot is absolute, so a re-write is idempotent, never double-counts).
            ps.markDossierDirty();
            safety.warn("tracker.flush.dossier", "uuid=" + s.uuid() + " " + res);
        }
        return true;
    }

    /** Flush changed compliance tallies for a player. Returns count of rows written. */
    private int flushCompliance(PlayerSignals ps) {
        Set<String> dirty = ps.drainDirtyComplianceKeys();
        if (dirty.isEmpty()) return 0;
        int n = 0;
        String now = SupabaseClient.timestampNow();
        for (String key : dirty) {
            SignalSnapshot.ComplianceTally t = ps.complianceFor(key);
            CustomComplianceRow row = new CustomComplianceRow(ps.uuid().toString(), ps.name(), key);
            row.honoredCount = t.honored();
            row.violatedCount = t.violated();
            row.lastEventAt = t.lastEventMs() > 0
                    ? java.time.Instant.ofEpochMilli(t.lastEventMs()).toString() : null;
            row.updatedAt = now;
            supabase.upsertCompliance(row);   // client queues on failure
            n++;
        }
        return n;
    }

    /** Flush dirty heatmap cells (absolute totals). Returns count of cells written. */
    private int flushHeatmap() {
        List<HeatmapAccumulator.CellSnapshot> snaps = heatmap.drainDirty();
        if (snaps.isEmpty()) return 0;
        String now = SupabaseClient.timestampNow();
        boolean anyHardFail = false;
        for (HeatmapAccumulator.CellSnapshot cs : snaps) {
            HeatmapCellRow row = new HeatmapCellRow(
                    cs.key.world, cs.key.cellX, cs.key.cellZ, cs.total, now);
            var res = supabase.upsertHeatmapCell(row);
            if (!res.ok() && (res.error() == null || !"queued-offline".equals(res.error()))) {
                anyHardFail = true;
            }
        }
        if (anyHardFail) {
            // Re-mark so we re-upsert absolute totals next cycle (idempotent; no double count).
            heatmap.markDirty(snaps);
        }
        return snaps.size();
    }

    /** Periodic base-detection pass — build + upsert base rows. ASYNC ONLY. */
    public int flushBases() {
        String now = SupabaseClient.timestampNow();
        List<BaseRow> rows = baseDetector.buildRows(now);
        for (BaseRow r : rows) {
            supabase.upsertBase(r);   // client queues on failure; id = owner uuid → idempotent
        }
        return rows.size();
    }

    /* ----------------------- row mapping ----------------------------- */

    private DossierRow toDossierRow(SignalSnapshot s) {
        DossierRow row = new DossierRow(s.uuid().toString(), s.name());
        row.soloMiningSeconds = s.soloMiningSeconds();
        row.deaths = s.deaths();
        row.blocksMined = s.blocksMined();
        row.hoardedScore = s.hoardedScore();
        row.distanceFromGroup = s.distanceFromGroup() < 0 ? null : s.distanceFromGroup();
        // Richer, schema-stable signals go in `extra` as a JSON string (lore-agnostic numbers).
        row.extra = buildExtraJson(s);
        row.updatedAt = SupabaseClient.timestampNow();
        return row;
    }

    /**
     * Build the {@code extra} JSON blob from measured numbers only. Hand-built (no Gson dependency
     * here) so it stays a flat, predictable, injection-safe object. All values are numbers/booleans
     * or a short location object — never free text, so there is nothing to escape beyond the world
     * name, which we JSON-escape.
     */
    private String buildExtraJson(SignalSnapshot s) {
        StringBuilder b = new StringBuilder(256);
        b.append('{');
        appendNum(b, "ores_mined", s.oresMined()); b.append(',');
        appendNum(b, "mob_kills", s.mobKills()); b.append(',');
        appendNum(b, "session_play_seconds", s.sessionPlaySeconds()); b.append(',');
        appendNum(b, "solo_mining_ratio", round3(s.soloMiningRatio())); b.append(',');
        appendNum(b, "deepest_y", s.deepestY()); b.append(',');
        appendNum(b, "forbidden_word_hits", s.forbiddenWordHits()); b.append(',');
        appendNum(b, "chat_sentiment", round3(s.chatSentiment())); b.append(',');
        appendNum(b, "chat_messages", s.chatMessages()); b.append(',');
        appendBool(b, "first_ore_taken", s.firstOreThisSessionTaken()); b.append(',');
        appendBool(b, "offering_honored_session", s.offeringHonoredThisSession()); b.append(',');
        // last-known location (for targeting / heatmap correlation) — null world ⇒ omit.
        if (s.lastWorld() != null && !s.lastWorld().isEmpty()) {
            b.append("\"last_loc\":{");
            b.append("\"world\":\"").append(jsonEscape(s.lastWorld())).append("\",");
            appendNum(b, "x", s.lastX()); b.append(',');
            appendNum(b, "y", s.lastY()); b.append(',');
            appendNum(b, "z", s.lastZ());
            b.append('}');
        } else {
            b.append("\"last_loc\":null");
        }
        b.append('}');
        return b.toString();
    }

    private static void appendNum(StringBuilder b, String k, long v) {
        b.append('"').append(k).append("\":").append(v);
    }
    private static void appendNum(StringBuilder b, String k, double v) {
        b.append('"').append(k).append("\":").append(v);
    }
    private static void appendBool(StringBuilder b, String k, boolean v) {
        b.append('"').append(k).append("\":").append(v);
    }
    private static double round3(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0;
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 4);
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

    private static String describe(Throwable t) {
        if (t == null) return "unknown";
        String m = t.getMessage();
        return t.getClass().getSimpleName() + (m == null ? "" : ": " + m);
    }

    /** Small summary of a flush pass (for debug logging). */
    public record FlushSummary(int dossiers, int compliance, int cells) {
        public int total() { return dossiers + compliance + cells; }
    }
}
