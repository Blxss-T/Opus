package com.opsflow.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Fails startup in the prod profile when a secret is missing or still set to
 * a value that is committed to the repository (dev defaults and the
 * .env.example placeholders).
 */
@Component
@Profile("prod")
public class ProductionSecretsValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionSecretsValidator.class);

    private static final Set<String> COMMITTED_JWT_SECRETS = Set.of(
        "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
        "change-me-to-a-unique-random-secret-at-least-32-chars"
    );
    private static final Set<String> COMMITTED_OTP_PEPPERS = Set.of(
        "opus-otp-dev-pepper",
        "change-me-otp-pepper"
    );
    private static final int MIN_JWT_SECRET_LENGTH = 32;
    private static final int MIN_OTP_PEPPER_LENGTH = 16;

    public ProductionSecretsValidator(
        @Value("${security.jwt.secret-key:}") String jwtSecret,
        @Value("${security.otp.pepper:}") String otpPepper,
        @Value("${security.google.client-id:}") String googleClientId
    ) {
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < MIN_JWT_SECRET_LENGTH) {
            throw new IllegalStateException("JWT_SECRET must be set to a unique value of at least 32 characters in production");
        }
        if (COMMITTED_JWT_SECRETS.contains(jwtSecret)) {
            throw new IllegalStateException("JWT_SECRET must not use a committed development or example value in production");
        }

        if (otpPepper == null || otpPepper.isBlank() || otpPepper.length() < MIN_OTP_PEPPER_LENGTH) {
            throw new IllegalStateException("OTP_PEPPER must be set to a unique value of at least 16 characters in production");
        }
        if (COMMITTED_OTP_PEPPERS.contains(otpPepper)) {
            throw new IllegalStateException("OTP_PEPPER must not use a committed development or example value in production");
        }

        // Not fatal: without a real client ID every Google token fails audience
        // verification, so the endpoint rejects sign-ins rather than accepting them.
        if (googleClientId == null || googleClientId.isBlank() || "dummy-client-id".equals(googleClientId)) {
            log.warn("GOOGLE_CLIENT_ID is not configured; Google sign-in will reject every token");
        }
    }
}
