package com.opsflow.auth.otp.service;

import com.opsflow.auth.otp.domain.OtpCode;
import com.opsflow.auth.otp.domain.OtpType;
import com.opsflow.auth.otp.repository.OtpRepository;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.common.mail.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpRepository otpRepository, EmailService emailService) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    @Transactional
    public OtpCode sendOtp(String email, OtpType type) {
        String cleanEmail = email.toLowerCase(Locale.ROOT).trim();
        OtpType otpType = type != null ? type : OtpType.EMAIL_VERIFICATION;

        String code = String.format("%06d", random.nextInt(1000000));
        Instant expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES);

        OtpCode otpCode = new OtpCode(cleanEmail, code, otpType, expiresAt);
        otpCode = otpRepository.save(otpCode);

        String subject = "OpsFlow - Verification Code";
        String body = String.format("""
            Hello,

            Your OpsFlow verification code is: %s

            This code will expire in 10 minutes. If you did not request this, please ignore this email.

            Regards,
            OpsFlow Engineering Team
            """, code);

        emailService.sendEmail(cleanEmail, subject, body);

        return otpCode;
    }

    @Transactional
    public boolean verifyOtp(String email, String code, OtpType type) {
        String cleanEmail = email.toLowerCase(Locale.ROOT).trim();
        OtpType otpType = type != null ? type : OtpType.EMAIL_VERIFICATION;

        OtpCode otpCode = otpRepository.findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(cleanEmail, otpType)
            .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid or expired OTP code"));

        if (otpCode.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "OTP code has expired");
        }

        if (!otpCode.getCode().equals(code.trim())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid OTP code");
        }

        otpCode.setUsed(true);
        otpRepository.save(otpCode);
        return true;
    }
}
