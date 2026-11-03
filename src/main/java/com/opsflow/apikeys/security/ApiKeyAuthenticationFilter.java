package com.opsflow.apikeys.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.apikeys.domain.ApiKeyScope;
import com.opsflow.apikeys.service.ApiKeyService;
import com.opsflow.apikeys.service.AuthenticatedApiKey;
import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.common.ratelimit.SlidingWindowRateLimiter;
import com.opsflow.common.response.ApiResponse;
import com.opsflow.common.response.ErrorResponse;
import com.opsflow.users.domain.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * Authenticates requests that carry an {@code X-API-Key} header.
 *
 * A key acts for its organization with MANAGER-level access, narrowed to its
 * scopes: the request's resource and method decide the scope it needs (see
 * {@link ApiKeyScope#requiredFor}), and endpoints with no scope are refused.
 * Records written by the key are attributed to "api-key:&lt;name&gt;".
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";
    public static final String ACTOR_PREFIX = "api-key:";

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;
    private final boolean rateLimitEnabled;
    private final long rateLimitWindowSeconds;
    private final SlidingWindowRateLimiter rateLimiter;

    public ApiKeyAuthenticationFilter(
        ApiKeyService apiKeyService,
        ObjectMapper objectMapper,
        @Value("${security.api-keys.rate-limit.enabled:true}") boolean rateLimitEnabled,
        @Value("${security.api-keys.rate-limit.limit:120}") int rateLimit,
        @Value("${security.api-keys.rate-limit.window-seconds:60}") long rateLimitWindowSeconds
    ) {
        this.apiKeyService = apiKeyService;
        this.objectMapper = objectMapper;
        this.rateLimitEnabled = rateLimitEnabled;
        this.rateLimitWindowSeconds = rateLimitWindowSeconds;
        this.rateLimiter = new SlidingWindowRateLimiter(rateLimit, rateLimitWindowSeconds);
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String rawKey = request.getHeader(HEADER);
        if (!StringUtils.hasText(rawKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<AuthenticatedApiKey> authenticated = apiKeyService.authenticate(rawKey.trim());
        if (authenticated.isEmpty()) {
            writeError(request, response, ErrorCode.UNAUTHORIZED, "API key is invalid, expired or revoked");
            return;
        }
        AuthenticatedApiKey apiKey = authenticated.get();

        // Per key, across all endpoints, so one integration cannot starve the rest.
        if (rateLimitEnabled && !rateLimiter.tryAcquire(apiKey.id().toString())) {
            log.warn("Rate limit exceeded for API key '{}' ({})", apiKey.name(), apiKey.id());
            response.setHeader("Retry-After", String.valueOf(rateLimitWindowSeconds));
            writeError(request, response, 429, "ERR_429", "Too many requests for this API key. Please try again later.");
            return;
        }

        Optional<ApiKeyScope> required = ApiKeyScope.requiredFor(request.getMethod(), request.getRequestURI());
        if (required.isEmpty()) {
            writeError(request, response, ErrorCode.FORBIDDEN, "This endpoint is not available to API keys");
            return;
        }
        if (!apiKey.allows(required.get())) {
            writeError(request, response, ErrorCode.FORBIDDEN,
                "API key is missing the required scope '" + required.get().getValue() + "'");
            return;
        }

        UserPrincipal principal = new UserPrincipal(
            apiKey.id(), apiKey.organizationId(), ACTOR_PREFIX + apiKey.name(), "", Role.MANAGER, true);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void writeError(
        HttpServletRequest request,
        HttpServletResponse response,
        ErrorCode errorCode,
        String message
    ) throws IOException {
        writeError(request, response, errorCode.getHttpStatus().value(), errorCode.getCode(), message);
    }

    private void writeError(
        HttpServletRequest request,
        HttpServletResponse response,
        int status,
        String code,
        String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(status)
            .error(code)
            .message(message)
            .path(request.getRequestURI())
            .timestamp(Instant.now())
            .build();

        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(errorResponse)));
    }
}
