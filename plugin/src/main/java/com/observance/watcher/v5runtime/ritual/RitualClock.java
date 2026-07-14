package com.observance.watcher.v5runtime.ritual;

/** Injectable monotonic/wall clock boundary for deterministic windows and restart tests. */
public interface RitualClock {
    long epochMillis();

    long tick();

    static RitualClock system() {
        return new RitualClock() {
            @Override
            public long epochMillis() {
                return System.currentTimeMillis();
            }

            @Override
            public long tick() {
                return System.nanoTime() / 50_000_000L;
            }
        };
    }
}
