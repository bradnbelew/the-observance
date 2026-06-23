package com.observance.watcher.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Supplier;

/**
 * TOTAL FAULT ISOLATION wrapper.
 *
 * <p>Every event-handler body and every scheduled-task body MUST run inside one of these
 * {@code run}/{@code call} methods. An exception is logged (console + Supabase {@code event_log}
 * via the {@link EventSink}) and SWALLOWED — never propagated. One bad beat must never crash
 * the plugin or the server.
 *
 * <p>Safety deliberately has NO hard dependency on SupabaseClient: it logs through an
 * {@link EventSink} functional interface that the plugin wires up after the client exists.
 * The sink itself is invoked defensively (its own failure is caught and dropped) so that a
 * broken logger can never re-enter and cause a loop. A re-entrancy guard prevents an
 * exception thrown *inside* the sink from triggering another sink call.
 */
public final class Safety {

    /**
     * Where structured error events go (normally an async Supabase {@code event_log} insert).
     * Implementations MUST be non-throwing and SHOULD be async/non-blocking; Safety calls this
     * from whatever thread the failing body was on.
     */
    @FunctionalInterface
    public interface EventSink {
        /**
         * @param type    short machine tag, e.g. "error" or "warn"
         * @param context the context string passed to the wrapped call (the call site name)
         * @param message human-readable detail (exception summary)
         */
        void log(String type, String context, String message);
    }

    private final Logger logger;
    private volatile EventSink sink;

    /** Re-entrancy guard so a throwing sink can't cause infinite logging recursion. */
    private final ThreadLocal<Boolean> inSink = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public Safety(Logger logger) {
        this.logger = logger;
        this.sink = null;
    }

    /** Wire (or rewire) the remote event sink. Safe to call before/after; null disables remote logging. */
    public void setEventSink(EventSink sink) {
        this.sink = sink;
    }

    /* ------------------------------------------------------------------ */
    /* Runnable wrappers                                                   */
    /* ------------------------------------------------------------------ */

    /**
     * Run a body, catching and logging ANY throwable. Never propagates.
     * @return true if the body completed normally, false if it threw.
     */
    public boolean run(String context, Runnable body) {
        if (body == null) return false;
        try {
            body.run();
            return true;
        } catch (Throwable t) {
            report(context, t);
            return false;
        }
    }

    /**
     * Call a supplier, catching and logging ANY throwable. Returns {@code fallback} on failure.
     * Never propagates.
     */
    public <T> T call(String context, Supplier<T> body, T fallback) {
        if (body == null) return fallback;
        try {
            return body.get();
        } catch (Throwable t) {
            report(context, t);
            return fallback;
        }
    }

    /**
     * Wrap a Runnable into a Safety-guarded Runnable (for passing to APIs that take a Runnable).
     */
    public Runnable wrap(String context, Runnable body) {
        return () -> run(context, body);
    }

    /* ------------------------------------------------------------------ */
    /* Reporting                                                           */
    /* ------------------------------------------------------------------ */

    /** Log a non-fatal warning event (no exception) to console + sink. Never throws. */
    public void warn(String context, String message) {
        try {
            logger.log(Level.WARNING, "[" + safe(context) + "] " + safe(message));
        } catch (Throwable ignored) { /* never propagate */ }
        emit("warn", context, message);
    }

    /** Log an informational event to console + sink. Never throws. */
    public void info(String context, String message) {
        try {
            logger.log(Level.INFO, "[" + safe(context) + "] " + safe(message));
        } catch (Throwable ignored) { /* never propagate */ }
        emit("info", context, message);
    }

    private void report(String context, Throwable t) {
        String msg = describe(t);
        try {
            logger.log(Level.SEVERE, "[" + safe(context) + "] swallowed: " + msg, t);
        } catch (Throwable ignored) { /* never propagate */ }
        emit("error", context, msg);
    }

    private void emit(String type, String context, String message) {
        EventSink s = this.sink;
        if (s == null) return;
        if (Boolean.TRUE.equals(inSink.get())) {
            // We're already inside a sink call (the sink itself failed and re-entered). Drop.
            return;
        }
        inSink.set(Boolean.TRUE);
        try {
            s.log(type, safe(context), safe(message));
        } catch (Throwable ignored) {
            // A failing sink must never crash anything. Drop silently.
        } finally {
            inSink.set(Boolean.FALSE);
        }
    }

    private static String describe(Throwable t) {
        if (t == null) return "unknown error";
        String name = t.getClass().getSimpleName();
        String m = t.getMessage();
        return (m == null || m.isBlank()) ? name : (name + ": " + m);
    }

    /** Null-safe + length-bounded so a giant string can't bloat the event_log row. */
    private static String safe(String s) {
        if (s == null) return "";
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }
}
