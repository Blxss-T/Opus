package com.opsflow.apikeys.dto;

/**
 * Returned once, when a key is created. {@code key} is the only time the raw
 * key is available; it cannot be retrieved afterwards.
 */
public record CreatedApiKeyResponse(
    String key,
    ApiKeyResponse apiKey
) {}
