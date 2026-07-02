package com.observance.watcher.npc;

import com.google.gson.JsonObject;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.rows.EventLogRow;
import com.observance.watcher.util.Safety;

import java.util.Map;

/**
 * THE REVEAL PRODUCER. Wren's reveal is TIED TO THE ISS CATCH, not a calendar (design §4 M4,
 * async-safe): once the group has caught Iss's lie ({@code iss_caught} becomes true in
 * {@code arc_state.flags}) — or independently finds the "kept close" artifact
 * ({@code companion_artifact_found}) — the same lens turns on Wren and {@code companion_revealed} is
 * set. This watcher polls {@code arc_state} on a slow cadence and fires the reveal flag ONCE,
 * idempotently, the first time either input is true and the reveal is not already set.
 *
 * <p>Setting {@code companion_revealed} is what unlocks his M4 reveal lines (the showrunner branches
 * its {@code companion.*} nodes on this flag, exactly as the Keeper branches on the dossier — no story
 * in the engine). This class is a pure flag PRODUCER: it reads state and writes one flag; it renders no
 * dialogue and makes no branch decision.
 *
 * <p>Fault-isolated by construction — a null/failed DB read leaves the flag untouched and simply
 * retries on the next poll. Runs ASYNC (DB I/O only; never touches a Bukkit world object).
 */
public final class CompanionArcWatcher {

    /** The gate inputs (either flips the reveal). */
    public static final String FLAG_ISS_CAUGHT       = "iss_caught";
    public static final String FLAG_ARTIFACT_FOUND   = "companion_artifact_found";
    /** The flag this watcher produces. */
    public static final String FLAG_REVEALED         = "companion_revealed";

    private final SupabaseClient supabase;
    private final Safety safety;

    /** In-memory latch so a successful set isn't re-attempted every poll (the DB is still the truth). */
    private volatile boolean revealedLatch = false;

    public CompanionArcWatcher(SupabaseClient supabase, Safety safety) {
        this.supabase = supabase;
        this.safety = safety;
    }

    /**
     * One poll: read arc_state; if the reveal isn't set yet AND (iss_caught OR artifact_found), set
     * {@code companion_revealed=true}. Idempotent and cheap; safe to call from a repeating async timer.
     */
    public void pollOnce() {
        if (supabase == null || revealedLatch) return;
        safety.run("companion.reveal.poll", () -> {
            var r = supabase.fetchArcState();
            if (r == null || !r.ok() || r.value() == null) return;
            Map<String, Object> flags = r.value().flagsMap();

            if (truthy(flags.get(FLAG_REVEALED))) {
                revealedLatch = true;   // already revealed elsewhere — stop polling
                return;
            }

            boolean issCaught = truthy(flags.get(FLAG_ISS_CAUGHT));
            boolean artifact  = truthy(flags.get(FLAG_ARTIFACT_FOUND));
            if (!issCaught && !artifact) return;   // gate not open yet

            JsonObject write = new JsonObject();
            write.addProperty(FLAG_REVEALED, true);
            supabase.mergeArcFlags(write);
            revealedLatch = true;

            String cause = issCaught ? "iss_caught" : "artifact_found";
            supabase.insertEventLog(new EventLogRow(
                    "companion", "reveal",
                    "Wren revealed (trigger: " + cause + ")",
                    null, "{\"trigger\":\"" + cause + "\"}", SupabaseClient.timestampNow()));
            safety.info("companion.reveal", "companion_revealed set (trigger=" + cause + ")");
        });
    }

    /** Reset the in-memory latch (e.g. on a hard reload rebuilding the watcher). */
    public void reset() { this.revealedLatch = false; }

    static boolean truthy(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0;
        if (v instanceof String s) {
            String t = s.trim();
            return t.equalsIgnoreCase("true") || t.equals("1");
        }
        return false;
    }
}
