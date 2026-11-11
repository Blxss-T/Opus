package com.opsflow.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record StepUpRequest(
    @NotBlank(message = "Password is required")
    String password
) {}
