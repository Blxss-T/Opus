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
import java.util.Set;

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

    private final SlidingWindowRateLimiter limiter;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final long windowSeconds;

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
        this.windowSeconds = windowSeconds;
        this.limiter = new SlidingWindowRateLimiter(limit, windowSeconds, clock);
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
        return limiter.tryAcquire(key);
    }

    /**
     * The X-Forwarded-For header is deliberately not read here: any client can
     * set it and get a fresh limit per request. Behind a reverse proxy, set
     * server.forward-headers-strategy=native so the container resolves the
     * real client address from trusted proxies into getRemoteAddr().
     */
    private String clientIp(HttpServletRequest request) {
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
