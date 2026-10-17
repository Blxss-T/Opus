package com.opsflow.apikeys.dto;

import com.opsflow.apikeys.domain.ApiKeyScope;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public record CreateApiKeyRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    String name,

    @NotEmpty(message = "At least one scope is required")
    Set<ApiKeyScope> scopes,

    @Future(message = "Expiry must be in the future")
    Instant expiresAt
) {}
