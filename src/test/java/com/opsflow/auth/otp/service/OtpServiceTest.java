package com.opsflow.auth.otp.service;

import com.opsflow.auth.otp.domain.OtpCode;
import com.opsflow.auth.otp.domain.OtpType;
import com.opsflow.auth.otp.repository.OtpRepository;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.mail.EmailService;
import com.opsflow.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpRepository otpRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OtpCodeGenerator otpCodeGenerator;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(otpRepository, emailService, userRepository, otpCodeGenerator, "test-pepper");
    }

    @Test
    void shouldHashAndDispatchOtpWithoutLoggingRawCodeThroughRepository() {
        when(otpCodeGenerator.generate()).thenReturn("123456");
        when(otpRepository.save(any(OtpCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpCode saved = otpService.sendOtp("User@Example.com", OtpType.EMAIL_VERIFICATION);

        ArgumentCaptor<OtpCode> captor = ArgumentCaptor.forClass(OtpCode.class);
        verify(otpRepository).save(captor.capture());
        assertEquals("user@example.com", captor.getValue().getEmail());
        assertEquals(otpService.hashCode("user@example.com", "123456"), captor.getValue().getCodeHash());
        verify(emailService).sendEmail(eq("user@example.com"), eq("Opus - Verification Code"), any());
        assertEquals("user@example.com", saved.getEmail());
    }

    @Test
    void shouldRejectExpiredCode() {
        OtpCode expired = new OtpCode("user@example.com", otpService.hashCode("user@example.com", "123456"),
            OtpType.EMAIL_VERIFICATION, Instant.now().minus(1, ChronoUnit.MINUTES));
        when(otpRepository.findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc("user@example.com", OtpType.EMAIL_VERIFICATION))
            .thenReturn(Optional.of(expired));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> otpService.verifyOtp("user@example.com", "123456", OtpType.EMAIL_VERIFICATION)
        );
        assertTrue(ex.getMessage().contains("expired"));
        assertTrue(expired.isUsed());
        verify(otpRepository).save(expired);
    }

    @Test
    void shouldRejectWrongCodeAndIncrementAttempts() {
        OtpCode otp = new OtpCode("user@example.com", otpService.hashCode("user@example.com", "123456"),
            OtpType.EMAIL_VERIFICATION, Instant.now().plus(10, ChronoUnit.MINUTES));
        when(otpRepository.findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc("user@example.com", OtpType.EMAIL_VERIFICATION))
            .thenReturn(Optional.of(otp));

        assertThrows(BusinessException.class, () -> otpService.verifyOtp("user@example.com", "000000", OtpType.EMAIL_VERIFICATION));
        assertEquals(1, otp.getAttemptCount());
        verify(userRepository, never()).findByEmail(any());
    }
}
