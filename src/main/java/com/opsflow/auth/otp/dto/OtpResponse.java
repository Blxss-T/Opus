package com.opsflow.auth.otp.dto;

import java.time.Instant;

public record OtpResponse(
    boolean success,
    String message,
    Instant expiresAt
) {}
