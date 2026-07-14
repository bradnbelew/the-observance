package com.observance.watcher.v5runtime;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Expiring token leases for sign editors, group rites, and other bounded interactions. */
public final class LeaseBook {
    private final ConcurrentHashMap<String, State> leases = new ConcurrentHashMap<>();
    private final LongSupplier nanoClock;

    public LeaseBook() {
        this(System::nanoTime);
    }

    LeaseBook(LongSupplier nanoClock) {
        this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
    }

    public Optional<Token> tryAcquire(String scope, String holder, Duration duration) {
        SiteMutexes.requireKey(scope, "scope");
        SiteMutexes.requireKey(holder, "holder");
        long durationNanos = positiveNanos(duration);
        long now = nanoClock.getAsLong();
        String tokenId = UUID.randomUUID().toString();
        State candidate = new State(holder, tokenId, saturatingAdd(now, durationNanos));
        State selected = leases.compute(scope, (ignored, current) -> {
            if (current == null || current.expiresAtNanos() <= now) {
                return candidate;
            }
            return current;
        });
        return selected == candidate
                ? Optional.of(new Token(this, scope, holder, tokenId))
                : Optional.empty();
    }

    public boolean renew(Token token, Duration duration) {
        Objects.requireNonNull(token, "token");
        token.requireOwner(this);
        long durationNanos = positiveNanos(duration);
        long now = nanoClock.getAsLong();
        final boolean[] renewed = {false};
        leases.computeIfPresent(token.scope(), (ignored, current) -> {
            if (!current.matches(token) || current.expiresAtNanos() <= now) {
                return current.expiresAtNanos() <= now ? null : current;
            }
            renewed[0] = true;
            return new State(current.holder(), current.tokenId(), saturatingAdd(now, durationNanos));
        });
        return renewed[0];
    }

    public boolean release(Token token) {
        Objects.requireNonNull(token, "token");
        token.requireOwner(this);
        final boolean[] removed = {false};
        leases.computeIfPresent(token.scope(), (ignored, current) -> {
            if (current.matches(token)) {
                removed[0] = true;
                return null;
            }
            return current;
        });
        if (removed[0]) {
            token.markClosed();
        }
        return removed[0];
    }

    public int purgeExpired() {
        long now = nanoClock.getAsLong();
        int before = leases.size();
        leases.entrySet().removeIf(entry -> entry.getValue().expiresAtNanos() <= now);
        return before - leases.size();
    }

    public Map<String, LeaseView> snapshot() {
        long now = nanoClock.getAsLong();
        Map<String, LeaseView> result = new LinkedHashMap<>();
        leases.forEach((scope, state) -> {
            if (state.expiresAtNanos() > now) {
                result.put(scope, new LeaseView(
                        scope, state.holder(), state.tokenId(), state.expiresAtNanos() - now));
            }
        });
        return Map.copyOf(result);
    }

    private static long positiveNanos(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("lease duration must be positive");
        }
        try {
            return duration.toNanos();
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    private static long saturatingAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    private record State(String holder, String tokenId, long expiresAtNanos) {
        private boolean matches(Token token) {
            return holder.equals(token.holder()) && tokenId.equals(token.tokenId());
        }
    }

    public record LeaseView(String scope, String holder, String tokenId, long remainingNanos) {
    }

    public static final class Token implements AutoCloseable {
        private final LeaseBook owner;
        private final String scope;
        private final String holder;
        private final String tokenId;
        private volatile boolean closed;

        private Token(LeaseBook owner, String scope, String holder, String tokenId) {
            this.owner = owner;
            this.scope = scope;
            this.holder = holder;
            this.tokenId = tokenId;
        }

        public String scope() {
            return scope;
        }

        public String holder() {
            return holder;
        }

        public String tokenId() {
            return tokenId;
        }

        public boolean closed() {
            return closed;
        }

        @Override
        public void close() {
            if (!closed) {
                owner.release(this);
            }
        }

        private void requireOwner(LeaseBook expected) {
            if (owner != expected) {
                throw new IllegalArgumentException("Lease token belongs to a different lease book");
            }
        }

        private void markClosed() {
            closed = true;
        }
    }
}
