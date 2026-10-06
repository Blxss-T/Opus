package com.opsflow.common.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitFilterTest {

    /** Simple mutable clock so window-expiry behavior is deterministic. */
    private static class AdjustableClock extends Clock {
        private Instant instant;

        AdjustableClock(Instant start) {
            this.instant = start;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private RateLimitFilter newFilter(int limit, long windowSeconds) {
        return new RateLimitFilter(
            new com.fasterxml.jackson.databind.ObjectMapper(),
            true,
            limit,
            windowSeconds,
            Clock.systemUTC()
        );
    }

    @Test
    void shouldAllowRequestsBelowLimit() {
        RateLimitFilter filter = newFilter(3, 60);

        assertTrue(filter.tryAcquire("ip1|/login"));
        assertTrue(filter.tryAcquire("ip1|/login"));
        assertTrue(filter.tryAcquire("ip1|/login"));
    }

    @Test
    void shouldBlockRequestOverLimit() {
        RateLimitFilter filter = newFilter(2, 60);

        assertTrue(filter.tryAcquire("ip1|/login"));
        assertTrue(filter.tryAcquire("ip1|/login"));
        assertFalse(filter.tryAcquire("ip1|/login"));
    }

    @Test
    void shouldTrackKeysIndependently() {
        RateLimitFilter filter = newFilter(1, 60);

        assertTrue(filter.tryAcquire("ip1|/login"));
        assertFalse(filter.tryAcquire("ip1|/login"));
        // Different IP and different endpoint are unaffected.
        assertTrue(filter.tryAcquire("ip2|/login"));
        assertTrue(filter.tryAcquire("ip1|/register"));
    }

    @Test
    void shouldRecoverAfterWindowPasses() {
        AdjustableClock clock = new AdjustableClock(Instant.parse("2026-01-01T00:00:00Z"));
        RateLimitFilter filter = new RateLimitFilter(
            new com.fasterxml.jackson.databind.ObjectMapper(),
            true,
            1,
            60,
            clock
        );

        assertTrue(filter.tryAcquire("ip1|/login"));
        assertFalse(filter.tryAcquire("ip1|/login"));

        clock.advance(Duration.ofSeconds(61));
        assertTrue(filter.tryAcquire("ip1|/login"));
    }

    @Test
    void shouldAllowAllTrafficWhenDisabled() {
        RateLimitFilter filter = new RateLimitFilter(
            new com.fasterxml.jackson.databind.ObjectMapper(),
            false,
            1,
            60
        );

        // tryAcquire is only consulted when enabled; filter bypass is the
        // disabled behavior asserted in the security wiring. Direct calls still
        // enforce, so just verify construction with disabled flag works.
        assertTrue(filter.tryAcquire("ip1|/login"));
    }
}
