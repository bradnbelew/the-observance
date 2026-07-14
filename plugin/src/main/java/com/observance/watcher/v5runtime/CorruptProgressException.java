package com.observance.watcher.v5runtime;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Indicates a fail-closed local record load. The original file is never replaced. */
public final class CorruptProgressException extends IOException {
    private final Path originalPath;
    private final Path recoveryCopy;

    CorruptProgressException(
            String message, Throwable cause, Path originalPath, Path recoveryCopy) {
        super(message, cause);
        this.originalPath = originalPath;
        this.recoveryCopy = recoveryCopy;
    }

    public Path originalPath() {
        return originalPath;
    }

    public Optional<Path> recoveryCopy() {
        return Optional.ofNullable(recoveryCopy);
    }
}
