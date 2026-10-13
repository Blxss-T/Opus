package com.opsflow.common.ratelimit;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window limiter: at most {@code limit} acquisitions per
 * key in any {@code windowSeconds} span.
 *
 * Single-node only; replace with a Redis-backed implementation when the app
 * scales beyond one instance.
 */
public class SlidingWindowRateLimiter {

    private static final int MAX_TRACKED_KEYS = 5000;

    private final Map<String, Deque<Instant>> windows = new ConcurrentHashMap<>();
    private final int limit;
    private final long windowSeconds;
    private final Clock clock;

    public SlidingWindowRateLimiter(int limit, long windowSeconds) {
        this(limit, windowSeconds, Clock.systemUTC());
    }

    public SlidingWindowRateLimiter(int limit, long windowSeconds, Clock clock) {
        this.limit = limit;
        this.windowSeconds = windowSeconds;
        this.clock = clock;
    }

    public boolean tryAcquire(String key) {
        boolean[] allowed = new boolean[1];
        windows.compute(key, (k, deque) -> {
            Instant now = clock.instant();
            Deque<Instant> window = deque != null ? deque : new ArrayDeque<>();
            purgeExpired(window, now);

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

    private void purgeExpired(Deque<Instant> window, Instant now) {
        while (!window.isEmpty() && window.peekFirst().plusSeconds(windowSeconds).isBefore(now)) {
            window.pollFirst();
        }
    }
}
