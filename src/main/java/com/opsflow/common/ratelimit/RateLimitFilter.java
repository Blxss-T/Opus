package com.opsflow.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.common.response.ApiResponse;
import com.opsflow.common.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window in-memory rate limiter for the public authentication endpoints
 * (password brute force, OTP flooding, reset-code enumeration).
 *
 * Single-node only; replace with Redis-backed limiting when the app scales
 * beyond one instance.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    static final Set<String> PROTECTED_PATHS = Set.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/google",
        "/api/v1/auth/otp/send",
        "/api/v1/auth/password/forgot",
        "/api/v1/auth/password/reset"
    );

    private static final int MAX_TRACKED_KEYS = 5000;

    private final Map<String, Deque<Instant>> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int limit;
    private final long windowSeconds;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public RateLimitFilter(
        ObjectMapper objectMapper,
        @Value("${security.rate-limit.enabled:true}") boolean enabled,
        @Value("${security.rate-limit.limit:10}") int limit,
        @Value("${security.rate-limit.window-seconds:60}") long windowSeconds
    ) {
        this(objectMapper, enabled, limit, windowSeconds, Clock.systemUTC());
    }

    RateLimitFilter(
        ObjectMapper objectMapper,
        boolean enabled,
        int limit,
        long windowSeconds,
        Clock clock
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.limit = limit;
        this.windowSeconds = windowSeconds;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        if (!enabled || !PROTECTED_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientIp(request) + "|" + request.getRequestURI();

        if (!tryAcquire(key)) {
            log.warn("Rate limit exceeded for {} on {}", clientIp(request), request.getRequestURI());
            writeTooManyRequests(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    boolean tryAcquire(String key) {
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

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(windowSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(429)
            .error("ERR_429")
            .message("Too many requests. Please try again later.")
            .path(request.getRequestURI())
            .timestamp(Instant.now())
            .build();

        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(errorResponse)));
    }
}
