package com.opsflow.auth.password;

import com.opsflow.auth.otp.domain.OtpType;
import com.opsflow.auth.otp.service.OtpService;
import com.opsflow.auth.session.service.RefreshTokenService;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.users.domain.User;
import com.opsflow.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

/**
 * Password reset via the existing OTP infrastructure (OtpType.PASSWORD_RESET).
 *
 * Forgot-password always responds 200 without revealing whether the account
 * exists; the reset itself requires a valid, unexpired, unused OTP code and
 * invalidates every session afterwards.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private static final String RESET_SUBJECT = "Opus - Password Reset Code";
    private static final String RESET_BODY = """
        Hello,

        We received a request to reset your Opus password.

        Your password reset code is: %s

        This code will expire in 10 minutes. If you did not request a reset, you can safely ignore this email.

        Regards,
        Opus Engineering Team
        """;

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public PasswordResetService(
        OtpService otpService,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        RefreshTokenService refreshTokenService
    ) {
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public void requestReset(String email) {
        String cleanEmail = email.toLowerCase(Locale.ROOT).trim();
        var userOpt = userRepository.findByEmail(cleanEmail);

        if (userOpt.isEmpty()) {
            // Do not reveal whether the account exists.
            log.info("Password reset requested for unknown address '{}'", cleanEmail);
            return;
        }

        otpService.sendOtp(cleanEmail, OtpType.PASSWORD_RESET, RESET_SUBJECT, RESET_BODY);
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        String cleanEmail = email.toLowerCase(Locale.ROOT).trim();

        User user = userRepository.findByEmail(cleanEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid or expired reset code"));

        if (!user.isActive() || !user.getOrganization().isActive()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Account or organization is inactive");
        }

        boolean verified = otpService.verifyOtp(cleanEmail, code, OtpType.PASSWORD_RESET);
        if (!verified) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid or expired reset code");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        // Any session (refresh family) is dead the moment credentials change.
        refreshTokenService.revokeAllForUser(user.getId());
        log.info("Password reset completed for user '{}'; all sessions revoked", user.getId());
    }
}
