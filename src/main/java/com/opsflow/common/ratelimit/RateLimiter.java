package com.opsflow.common.ratelimit;

/**
 * Fixed-window counter: at most {@code limit} acquisitions per {@code key}
 * in a {@code windowSeconds} span.
 */
public interface RateLimiter {

    boolean tryAcquire(String key, int limit, long windowSeconds);
}
