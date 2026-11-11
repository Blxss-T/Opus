package com.opsflow.common.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;

/**
 * Redis fixed-window counters. Falls back to an in-memory limiter when Redis
 * is missing or errors, unless {@code failClosed} is true (production).
 */
public class RedisBackedRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisBackedRateLimiter.class);

    private final StringRedisTemplate redis;
    private final RateLimiter fallback;
    private final boolean failClosed;

    public RedisBackedRateLimiter(StringRedisTemplate redis, RateLimiter fallback, boolean failClosed) {
        this.redis = redis;
        this.fallback = fallback;
        this.failClosed = failClosed;
    }

    @Override
    public boolean tryAcquire(String key, int limit, long windowSeconds) {
        if (redis == null) {
            return fallback.tryAcquire(key, limit, windowSeconds);
        }
        try {
            long window = Instant.now().getEpochSecond() / Math.max(windowSeconds, 1);
            String redisKey = "rl:" + windowSeconds + ":" + window + ":" + key;
            Long count = redis.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redis.expire(redisKey, Duration.ofSeconds(windowSeconds + 1));
            }
            return count != null && count <= limit;
        } catch (RuntimeException ex) {
            log.warn("Rate limiter Redis error: {}", ex.getMessage());
            if (failClosed) {
                return false;
            }
            return fallback.tryAcquire(key, limit, windowSeconds);
        }
    }
}
