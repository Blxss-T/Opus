package com.opsflow.auth.session.service;

import com.opsflow.users.domain.User;
import com.opsflow.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private com.opsflow.auth.session.repository.RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, 60 * 24 * 7);
    }

    private User mockUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        return user;
    }

    @Test
    void issuedTokenShouldNotPersistRawValue() {
        User user = mockUser();
        when(refreshTokenRepository.save(any(com.opsflow.auth.session.domain.RefreshToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken = refreshTokenService.issueToken(user);

        assertThat(rawToken).hasSize(64).matches("[0-9a-f]+");

        org.mockito.ArgumentCaptor<com.opsflow.auth.session.domain.RefreshToken> captor =
            org.mockito.ArgumentCaptor.forClass(com.opsflow.auth.session.domain.RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        com.opsflow.auth.session.domain.RefreshToken saved = captor.getValue();
        assertThat(saved.getTokenHash()).isEqualTo(RefreshTokenService.sha256(rawToken));
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(saved.isRevoked()).isFalse();
    }

    @Test
    void consumingValidTokenShouldRotateIt() {
        User user = mockUser();
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.sha256("any-raw"))).thenReturn(
            Optional.of(new com.opsflow.auth.session.domain.RefreshToken(user, RefreshTokenService.sha256("any-raw"),
                java.time.Instant.now().plusSeconds(3600)))
        );

        User consumed = refreshTokenService.consumeToken("any-raw");

        assertThat(consumed).isSameAs(user);
        org.mockito.ArgumentCaptor<com.opsflow.auth.session.domain.RefreshToken> captor =
            org.mockito.ArgumentCaptor.forClass(com.opsflow.auth.session.domain.RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().isRevoked()).isTrue();
    }

    @Test
    void consumingRevokedTokenShouldRevokeWholeFamily() {
        User user = mockUser();
        com.opsflow.auth.session.domain.RefreshToken revoked =
            new com.opsflow.auth.session.domain.RefreshToken(user, RefreshTokenService.sha256("any-raw"),
                java.time.Instant.now().plusSeconds(3600));
        revoked.setRevoked(true);
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.sha256("any-raw"))).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refreshTokenService.consumeToken("any-raw"))
            .isInstanceOf(RefreshTokenService.RevokedRefreshTokenException.class);

        verify(refreshTokenRepository).revokeAllForUser(user.getId());
    }

    @Test
    void consumingExpiredTokenShouldRevokeWholeFamily() {
        User user = mockUser();
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.sha256("any-raw"))).thenReturn(
            Optional.of(new com.opsflow.auth.session.domain.RefreshToken(user, RefreshTokenService.sha256("any-raw"),
                java.time.Instant.now().minusSeconds(60)))
        );

        assertThatThrownBy(() -> refreshTokenService.consumeToken("any-raw"))
            .isInstanceOf(RefreshTokenService.RevokedRefreshTokenException.class);

        verify(refreshTokenRepository).revokeAllForUser(user.getId());
    }

    @Test
    void unknownTokenShouldBeRejectedWithoutRevocation() {
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.sha256("any-raw"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.consumeToken("any-raw"))
            .isInstanceOf(RefreshTokenService.RevokedRefreshTokenException.class);

        verify(refreshTokenRepository, org.mockito.Mockito.never()).revokeAllForUser(any());
    }
}
