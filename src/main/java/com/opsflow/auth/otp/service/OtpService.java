package com.opsflow.auth.otp.service;

import com.opsflow.auth.otp.domain.OtpCode;
import com.opsflow.auth.otp.domain.OtpType;
import com.opsflow.auth.otp.repository.OtpRepository;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.common.mail.EmailService;
import com.opsflow.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    static final int MAX_ATTEMPTS = 5;
    static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);

    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final OtpCodeGenerator otpCodeGenerator;
    private final String otpPepper;
    private final Map<String, Instant> lastSendByKey = new ConcurrentHashMap<>();

    public OtpService(
        OtpRepository otpRepository,
        EmailService emailService,
        UserRepository userRepository,
        OtpCodeGenerator otpCodeGenerator,
        @Value("${security.otp.pepper:opus-otp-dev-pepper}") String otpPepper
    ) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.otpCodeGenerator = otpCodeGenerator;
        this.otpPepper = otpPepper;
    }

    @Transactional
    public OtpCode sendOtp(String email, OtpType type) {
        String cleanEmail = email.toLowerCase(Locale.ROOT).trim();
        OtpType otpType = type != null ? type : OtpType.EMAIL_VERIFICATION;
        enforceSendCooldown(cleanEmail, otpType);

        otpRepository.invalidateUnusedCodes(cleanEmail, otpType);

        String rawCode = otpCodeGenerator.generate();
        Instant expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES);
        OtpCode otpCode = new OtpCode(cleanEmail, hashCode(cleanEmail, rawCode), otpType, expiresAt);
        otpCode = otpRepository.save(otpCode);

        String subject = "Opus - Verification Code";
        String body = """
            Hello,

            Your Opus verification code is: %s

            This code will expire in 10 minutes. If you did not request this, please ignore this email.

            Regards,
            Opus Engineering Team
            """.formatted(rawCode);

        emailService.sendEmail(cleanEmail, subject, body);
        lastSendByKey.put(cooldownKey(cleanEmail, otpType), Instant.now());
        return otpCode;
    }

    @Transactional
    public boolean verifyOtp(String email, String code, OtpType type) {
        String cleanEmail = email.toLowerCase(Locale.ROOT).trim();
        OtpType otpType = type != null ? type : OtpType.EMAIL_VERIFICATION;

        OtpCode otpCode = otpRepository.findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(cleanEmail, otpType)
            .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid or expired OTP code"));

        if (otpCode.getExpiresAt().isBefore(Instant.now())) {
            otpCode.setUsed(true);
            otpRepository.save(otpCode);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "OTP code has expired");
        }

        if (otpCode.getAttemptCount() >= MAX_ATTEMPTS) {
            otpCode.setUsed(true);
            otpRepository.save(otpCode);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "OTP code has been locked after too many attempts");
        }

        if (!otpCode.getCodeHash().equals(hashCode(cleanEmail, code.trim()))) {
            otpCode.setAttemptCount(otpCode.getAttemptCount() + 1);
            if (otpCode.getAttemptCount() >= MAX_ATTEMPTS) {
                otpCode.setUsed(true);
            }
            otpRepository.save(otpCode);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid OTP code");
        }

        otpCode.setUsed(true);
        otpRepository.save(otpCode);

        if (otpType == OtpType.EMAIL_VERIFICATION) {
            userRepository.findByEmail(cleanEmail).ifPresent(user -> {
                if (user.getEmailVerifiedAt() == null) {
                    user.setEmailVerifiedAt(Instant.now());
                    userRepository.save(user);
                }
            });
        }

        return true;
    }

    String hashCode(String email, String rawCode) {
        String material = otpPepper + ":" + email + ":" + rawCode;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private void enforceSendCooldown(String email, OtpType type) {
        Instant lastSend = lastSendByKey.get(cooldownKey(email, type));
        if (lastSend != null && lastSend.plus(SEND_COOLDOWN).isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Please wait before requesting another OTP");
        }
    }

    private String cooldownKey(String email, OtpType type) {
        return email + ":" + type.name();
    }
}
