package com.opsflow.apikeys.service;

import com.opsflow.apikeys.domain.ApiKey;
import com.opsflow.apikeys.dto.ApiKeyResponse;
import com.opsflow.apikeys.dto.CreateApiKeyRequest;
import com.opsflow.apikeys.dto.CreatedApiKeyResponse;
import com.opsflow.apikeys.repository.ApiKeyRepository;
import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.ResourceNotFoundException;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApiKeyService {

    public static final String KEY_PREFIX = "opk_";

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private static final int KEY_BYTES = 32;
    private static final int DISPLAY_PREFIX_LENGTH = 12;
    private static final Duration LAST_USED_RESOLUTION = Duration.ofMinutes(1);

    private final ApiKeyRepository apiKeyRepository;
    private final OrganizationRepository organizationRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository, OrganizationRepository organizationRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public CreatedApiKeyResponse createApiKey(UserPrincipal principal, CreateApiKeyRequest request) {
        UUID organizationId = principal.getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        byte[] random = new byte[KEY_BYTES];
        secureRandom.nextBytes(random);
        String rawKey = KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(random);

        ApiKey apiKey = apiKeyRepository.save(new ApiKey(
            organization,
            request.name().trim(),
            rawKey.substring(0, DISPLAY_PREFIX_LENGTH),
            hash(rawKey),
            request.scopes(),
            principal.getId(),
            principal.getUsername(),
            request.expiresAt()
        ));

        log.info("API key '{}' ({}) created for organization '{}' by '{}'",
            apiKey.getName(), apiKey.getKeyPrefix(), organizationId, principal.getUsername());
        return new CreatedApiKeyResponse(rawKey, ApiKeyResponse.fromEntity(apiKey));
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listApiKeys(UserPrincipal principal) {
        return apiKeyRepository.findByOrganizationIdOrderByCreatedAtDesc(principal.getOrganizationId()).stream()
            .map(ApiKeyResponse::fromEntity)
            .toList();
    }

    @Transactional
    public ApiKeyResponse revokeApiKey(UserPrincipal principal, UUID apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findByIdAndOrganizationId(apiKeyId, principal.getOrganizationId())
            .orElseThrow(() -> new ResourceNotFoundException("ApiKey", "id", apiKeyId));

        if (apiKey.getRevokedAt() == null) {
            apiKey.setRevokedAt(Instant.now());
            apiKey = apiKeyRepository.save(apiKey);
            log.info("API key '{}' ({}) revoked by '{}'", apiKey.getName(), apiKey.getKeyPrefix(), principal.getUsername());
        }
        return ApiKeyResponse.fromEntity(apiKey);
    }

    /**
     * Resolves a raw key presented by a client. Empty when the key is
     * unknown, revoked, expired, or belongs to an inactive organization.
     */
    @Transactional
    public Optional<AuthenticatedApiKey> authenticate(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(KEY_PREFIX)) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        Optional<ApiKey> found = apiKeyRepository.findByKeyHash(hash(rawKey)).filter(key -> key.isUsable(now));
        if (found.isEmpty()) {
            return Optional.empty();
        }

        ApiKey apiKey = found.get();
        // Avoids a database write on every request from a busy integration.
        if (apiKey.getLastUsedAt() == null || apiKey.getLastUsedAt().plus(LAST_USED_RESOLUTION).isBefore(now)) {
            apiKey.setLastUsedAt(now);
            apiKeyRepository.save(apiKey);
        }

        return Optional.of(new AuthenticatedApiKey(
            apiKey.getId(), apiKey.getOrganization().getId(), apiKey.getName(), apiKey.getScopes()));
    }

    static String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawKey.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
