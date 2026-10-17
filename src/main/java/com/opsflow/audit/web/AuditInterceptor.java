package com.opsflow.audit.web;

import com.opsflow.audit.service.AuditService;
import com.opsflow.auth.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.UUID;

/**
 * Records every successful change made by an authenticated caller: any
 * non-read request under /api/v1 that ended in a 2xx status. Reads, failed
 * requests and unauthenticated calls (login, registration) are not recorded.
 *
 * Recording is best effort: the change has already been committed when this
 * runs, so a failure to write the audit row is logged rather than turned
 * into an error response for a request that actually succeeded.
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuditInterceptor.class);
    private static final String API_PREFIX = "/api/v1/";
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final AuditService auditService;

    public AuditInterceptor(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void afterCompletion(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull Object handler,
        Exception ex
    ) {
        String path = request.getRequestURI();
        int status = response.getStatus();
        if (READ_METHODS.contains(request.getMethod()) || !path.startsWith(API_PREFIX) || status < 200 || status >= 300) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return;
        }

        String[] segments = path.substring(API_PREFIX.length()).split("/");
        try {
            auditService.record(
                principal,
                request.getMethod(),
                path,
                segments[0],
                resourceId(segments),
                status,
                request.getRemoteAddr()
            );
        } catch (RuntimeException e) {
            log.error("Failed to record audit event for {} {}", request.getMethod(), path, e);
        }
    }

    /** The id in /resource/{id}/..., when the second path segment is one. */
    private UUID resourceId(String[] segments) {
        if (segments.length < 2) {
            return null;
        }
        try {
            return UUID.fromString(segments[1]);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
