package com.observance.watcher.v5runtime;

/** Thrown when the packaged V5 predicate authority is incomplete, malformed, or inconsistent. */
public final class AuthorityException extends IllegalStateException {
    public AuthorityException(String message) {
        super(message);
    }

    public AuthorityException(String message, Throwable cause) {
        super(message, cause);
    }
}
