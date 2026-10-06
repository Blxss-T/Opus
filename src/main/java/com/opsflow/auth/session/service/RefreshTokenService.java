package com.opsflow.auth.session.service;

import com.opsflow.auth.session.domain.RefreshToken;
import com.opsflow.auth.session.repository.RefreshTokenRepository;
import com.opsflow.users.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Stateful refresh token sessions with rotation and reuse detection:
 * presenting a token that has already been rotated or revoked revokes the
 * entire family for that user, which is the standard mitigation for stolen
 * refresh tokens.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
        RefreshTokenRepository refreshTokenRepository,
        @Value("${security.refresh-token.ttl:#{60 * 24 * 7}}") long refreshTtlMinutes
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTtl = Duration.ofMinutes(refreshTtlMinutes);
    }

    /**
     * Issues a fresh refresh token for the user. The raw token is returned to
     * the client exactly once; only its SHA-256 hash is persisted.
     */
    @Transactional
    public String issueToken(User user) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = HexFormat.of().formatHex(randomBytes);

        RefreshToken refreshToken = new RefreshToken(user, sha256(rawToken), Instant.now().plus(refreshTtl));
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    /**
     * Validates the presented refresh token. Revoked, expired, or
     * already-rotated tokens throw {@link RevokedRefreshTokenException} and
     * revoke every remaining session for the user (reuse detection).
     */
    @Transactional
    public User consumeToken(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(sha256(rawToken))
            .orElseThrow(RevokedRefreshTokenException::new);

        boolean previouslyUsed = refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(Instant.now());

        if (previouslyUsed) {
            // Reuse of an old token: treat as theft and kill the whole family.
            refreshTokenRepository.revokeAllForUser(refreshToken.getUser().getId());
            log.warn("Refresh token reuse detected for user {}; all sessions revoked", refreshToken.getUser().getId());
            throw new RevokedRefreshTokenException();
        }

        // Single use: rotate by revoking the presented token.
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return refreshToken.getUser();
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    public static String sha256(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public static class RevokedRefreshTokenException extends RuntimeException {
        public RevokedRefreshTokenException() {
            super("Invalid or expired refresh token");
        }
    }
}
