package com.observance.watcher.data;

/**
 * Result of a Supabase call. NEVER throws into callers. {@code ok} false means the call failed
 * (network/HTTP/parse) and the plugin should degrade gracefully — callers must tolerate this.
 *
 * @param <T> parsed body type (e.g. a List of rows), or Void for writes.
 */
public final class SupabaseResult<T> {

    private final boolean ok;
    private final int httpStatus;     // 0 when no HTTP response (network failure)
    private final T value;            // nullable
    private final String error;       // nullable; short reason on failure

    private SupabaseResult(boolean ok, int httpStatus, T value, String error) {
        this.ok = ok;
        this.httpStatus = httpStatus;
        this.value = value;
        this.error = error;
    }

    public static <T> SupabaseResult<T> ok(int status, T value) {
        return new SupabaseResult<>(true, status, value, null);
    }

    public static <T> SupabaseResult<T> fail(int status, String error) {
        return new SupabaseResult<>(false, status, null, error);
    }

    /** A failure that was deliberately queued for later (offline degrade). */
    public static <T> SupabaseResult<T> queued() {
        return new SupabaseResult<>(false, 0, null, "queued-offline");
    }

    public boolean ok() { return ok; }
    public int httpStatus() { return httpStatus; }
    public T value() { return value; }
    public String error() { return error; }

    @Override
    public String toString() {
        return ok ? ("ok(" + httpStatus + ")") : ("fail(" + httpStatus + "," + error + ")");
    }
}
