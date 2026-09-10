package com.opsflow.auth.otp.dto;

import com.opsflow.auth.otp.domain.OtpType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "OTP code is required")
    String code,

    OtpType type
) {}
