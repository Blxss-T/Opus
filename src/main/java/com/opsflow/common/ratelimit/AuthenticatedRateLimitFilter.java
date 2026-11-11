package com.opsflow.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.response.ApiResponse;
import com.opsflow.common.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Per authenticated session (organization + user) after JWT/API-key auth.
 * API keys also have a dedicated per-key limiter in {@code ApiKeyAuthenticationFilter}.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class AuthenticatedRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthenticatedRateLimitFilter.class);

    private final RateLimiter limiter;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int limit;
    private final long windowSeconds;

    public AuthenticatedRateLimitFilter(
        RateLimiter limiter,
        ObjectMapper objectMapper,
        @Value("${security.rate-limit.enabled:true}") boolean enabled,
        @Value("${security.rate-limit.authenticated-limit:120}") int limit,
        @Value("${security.rate-limit.window-seconds:60}") long windowSeconds
    ) {
        this.limiter = limiter;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.limit = limit;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (!enabled || RateLimitFilter.PROTECTED_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = "user:" + principal.getOrganizationId() + ":" + principal.getId();
        if (!limiter.tryAcquire(key, limit, windowSeconds)) {
            log.warn("Authenticated rate limit exceeded for user {} org {}", principal.getId(), principal.getOrganizationId());
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
            return;
        }

        filterChain.doFilter(request, response);
    }
}
