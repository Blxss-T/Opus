package com.opsflow.common.mail;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailServiceTest {

    @Test
    void shouldSendThroughMailSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService emailService = new EmailService(mailSender, true);

        emailService.sendEmail("user@example.com", "Subject", "Body");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldThrowWhenDeliveryFailsAndFailOnErrorIsEnabled() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        EmailService emailService = new EmailService(mailSender, true);

        assertThrows(EmailDeliveryException.class,
            () -> emailService.sendEmail("user@example.com", "Subject", "Body"));
    }

    @Test
    void shouldThrowWhenNoMailServerIsConfiguredAndFailOnErrorIsEnabled() {
        EmailService emailService = new EmailService(null, true);

        assertThrows(EmailDeliveryException.class,
            () -> emailService.sendEmail("user@example.com", "Subject", "Body"));
    }

    @Test
    void shouldFallBackToLoggingWhenFailOnErrorIsDisabled() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> new EmailService(mailSender, false).sendEmail("user@example.com", "Subject", "Body"));
        assertDoesNotThrow(() -> new EmailService(null, false).sendEmail("user@example.com", "Subject", "Body"));
    }
}
