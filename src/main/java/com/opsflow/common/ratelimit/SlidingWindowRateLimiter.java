package com.opsflow.common.ratelimit;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window limiter used in tests and as a fallback when Redis
 * is unavailable and fail-open is configured.
 */
public class SlidingWindowRateLimiter implements RateLimiter {

    private static final int MAX_TRACKED_KEYS = 5000;

    private final Map<String, Deque<Instant>> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public SlidingWindowRateLimiter() {
        this(Clock.systemUTC());
    }

    public SlidingWindowRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * @deprecated Per-instance limit/window; prefer {@link #tryAcquire(String, int, long)}.
     */
    @Deprecated
    public SlidingWindowRateLimiter(int limit, long windowSeconds) {
        this(Clock.systemUTC());
    }

    @Deprecated
    public SlidingWindowRateLimiter(int limit, long windowSeconds, Clock clock) {
        this(clock);
    }

    @Deprecated
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 10, 60);
    }

    @Override
    public boolean tryAcquire(String key, int limit, long windowSeconds) {
        boolean[] allowed = new boolean[1];
        windows.compute(key, (k, deque) -> {
            Instant now = clock.instant();
            Deque<Instant> window = deque != null ? deque : new ArrayDeque<>();
            while (!window.isEmpty() && window.peekFirst().plusSeconds(windowSeconds).isBefore(now)) {
                window.pollFirst();
            }

            allowed[0] = window.size() < limit;
            if (allowed[0]) {
                window.addLast(now);
            }
            return window.isEmpty() ? null : window;
        });

        if (windows.size() > MAX_TRACKED_KEYS) {
            Instant now = clock.instant();
            windows.entrySet().removeIf(entry -> {
                Deque<Instant> deque = entry.getValue();
                return deque == null || deque.isEmpty()
                    || deque.peekLast().plusSeconds(windowSeconds).isBefore(now);
            });
        }

        return allowed[0];
    }
}
