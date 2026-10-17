package com.opsflow.apikeys.dto;

import com.opsflow.apikeys.domain.ApiKey;
import com.opsflow.apikeys.domain.ApiKeyScope;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ApiKeyResponse(
    UUID id,
    String name,
    String keyPrefix,
    Set<ApiKeyScope> scopes,
    boolean active,
    String createdByEmail,
    Instant createdAt,
    Instant expiresAt,
    Instant revokedAt,
    Instant lastUsedAt
) {
    public static ApiKeyResponse fromEntity(ApiKey apiKey) {
        return new ApiKeyResponse(
            apiKey.getId(),
            apiKey.getName(),
            apiKey.getKeyPrefix(),
            apiKey.getScopes(),
            apiKey.isUsable(Instant.now()),
            apiKey.getCreatedByEmail(),
            apiKey.getCreatedAt(),
            apiKey.getExpiresAt(),
            apiKey.getRevokedAt(),
            apiKey.getLastUsedAt()
        );
    }
}
