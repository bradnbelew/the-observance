package com.observance.watcher.v5runtime;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/** Fair, site-keyed mutexes for serializing physical predicate transactions. */
public final class SiteMutexes {
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public Optional<Guard> tryAcquire(String siteId, Duration timeout) throws InterruptedException {
        requireKey(siteId, "siteId");
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("timeout cannot be negative");
        }
        ReentrantLock lock = locks.computeIfAbsent(siteId, ignored -> new ReentrantLock(true));
        long timeoutNanos;
        try {
            timeoutNanos = timeout.toNanos();
        } catch (ArithmeticException exception) {
            timeoutNanos = Long.MAX_VALUE;
        }
        boolean acquired = lock.tryLock(timeoutNanos, TimeUnit.NANOSECONDS);
        return acquired ? Optional.of(new Guard(siteId, lock)) : Optional.empty();
    }

    public int registeredSiteCount() {
        return locks.size();
    }

    public static final class Guard implements AutoCloseable {
        private final String siteId;
        private final ReentrantLock lock;
        private boolean closed;

        private Guard(String siteId, ReentrantLock lock) {
            this.siteId = siteId;
            this.lock = lock;
        }

        public String siteId() {
            return siteId;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (!lock.isHeldByCurrentThread()) {
                throw new IllegalStateException("Site mutex must be released by its acquiring thread");
            }
            closed = true;
            lock.unlock();
        }
    }

    static void requireKey(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
    }
}
