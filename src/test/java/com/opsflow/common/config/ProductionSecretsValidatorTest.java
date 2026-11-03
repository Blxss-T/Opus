package com.opsflow.common.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionSecretsValidatorTest {

    private static final String VALID_JWT_SECRET = "a-unique-production-secret-of-sufficient-length";
    private static final String VALID_OTP_PEPPER = "a-unique-production-pepper";
    private static final String GOOGLE_CLIENT_ID = "1234.apps.googleusercontent.com";

    @Test
    void shouldAcceptUniqueSecrets() {
        assertDoesNotThrow(() -> new ProductionSecretsValidator(VALID_JWT_SECRET, VALID_OTP_PEPPER, GOOGLE_CLIENT_ID));
    }

    @Test
    void shouldNotFailStartupWhenGoogleClientIdIsMissing() {
        assertDoesNotThrow(() -> new ProductionSecretsValidator(VALID_JWT_SECRET, VALID_OTP_PEPPER, "dummy-client-id"));
        assertDoesNotThrow(() -> new ProductionSecretsValidator(VALID_JWT_SECRET, VALID_OTP_PEPPER, ""));
    }

    @Test
    void shouldRejectMissingShortOrCommittedJwtSecret() {
        assertRejected("JWT_SECRET", "", VALID_OTP_PEPPER);
        assertRejected("JWT_SECRET", "too-short", VALID_OTP_PEPPER);
        assertRejected("JWT_SECRET", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", VALID_OTP_PEPPER);
        assertRejected("JWT_SECRET", "change-me-to-a-unique-random-secret-at-least-32-chars", VALID_OTP_PEPPER);
    }

    @Test
    void shouldRejectMissingShortOrCommittedOtpPepper() {
        assertRejected("OTP_PEPPER", VALID_JWT_SECRET, "");
        assertRejected("OTP_PEPPER", VALID_JWT_SECRET, "short");
        assertRejected("OTP_PEPPER", VALID_JWT_SECRET, "opus-otp-dev-pepper");
        assertRejected("OTP_PEPPER", VALID_JWT_SECRET, "change-me-otp-pepper");
    }

    private void assertRejected(String expectedVariable, String jwtSecret, String otpPepper) {
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> new ProductionSecretsValidator(jwtSecret, otpPepper, GOOGLE_CLIENT_ID)
        );
        assertTrue(ex.getMessage().startsWith(expectedVariable), "Unexpected message: " + ex.getMessage());
    }
}
