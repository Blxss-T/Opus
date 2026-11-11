package com.opsflow.auth.security;

import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.Instant;

@Component
public class StepUpGuard {

    private final boolean enabled;

    public StepUpGuard(@Value("${security.step-up.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    public void require(UserPrincipal principal) {
        if (!enabled || RequestContextHolder.getRequestAttributes() == null) {
            return;
        }
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Unauthenticated request");
        }
        if (principal.isApiKey()) {
            return;
        }
        Instant expiresAt = principal.getReauthExpiresAt();
        if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Re-authentication required");
        }
    }
}
