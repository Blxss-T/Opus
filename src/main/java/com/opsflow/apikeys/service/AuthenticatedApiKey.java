package com.opsflow.apikeys.service;

import com.opsflow.apikeys.domain.ApiKeyScope;

import java.util.Set;
import java.util.UUID;

/** A verified API key, detached from the persistence context. */
public record AuthenticatedApiKey(
    UUID id,
    UUID organizationId,
    String name,
    Set<ApiKeyScope> scopes
) {
    public boolean allows(ApiKeyScope required) {
        return scopes.stream().anyMatch(scope -> scope.satisfies(required));
    }
}
