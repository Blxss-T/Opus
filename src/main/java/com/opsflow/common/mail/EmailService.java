package com.opsflow.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final boolean failOnError;

    public EmailService(
        @Autowired(required = false) JavaMailSender mailSender,
        @Value("${opus.mail.fail-on-error:false}") boolean failOnError
    ) {
        this.mailSender = mailSender;
        this.failOnError = failOnError;
    }

    /**
     * Sends a plain-text email. When {@code opus.mail.fail-on-error} is true
     * (the prod default) a missing SMTP configuration or a delivery failure
     * throws {@link EmailDeliveryException}; otherwise it falls back to
     * logging so local development works without a mail server.
     */
    public void sendEmail(String to, String subject, String body) {
        if (mailSender == null) {
            if (failOnError) {
                throw new EmailDeliveryException("No mail server is configured (MAIL_HOST is not set)");
            }
            logFallback(to, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to '{}'", to);
        } catch (Exception ex) {
            if (failOnError) {
                throw new EmailDeliveryException("SMTP mail delivery failed", ex);
            }
            log.warn("SMTP mail delivery failed, falling back to log mode: {}", ex.getMessage());
            logFallback(to, subject);
        }
    }

    private void logFallback(String to, String subject) {
        log.info("Dev email fallback: message queued for '{}' with subject '{}' (body omitted)", to, subject);
    }
}
